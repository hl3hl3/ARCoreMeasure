package com.hl3hl3.arcoremeasure

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.MotionEvent
import com.Logger
import com.google.ar.core.*
import com.google.ar.core.examples.java.helloar.DisplayRotationHelper
import com.google.ar.core.examples.java.helloar.rendering.BackgroundRenderer
import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer
import com.google.ar.core.examples.java.helloar.rendering.PlaneRenderer
import com.google.ar.core.examples.java.helloar.rendering.PointCloudRenderer
import com.hl3hl3.arcoremeasure.renderer.RectanglePolygonRenderer
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLSurfaceRenderer(
    private val context: Context,
    private val session: Session,
    private val displayRotationHelper: DisplayRotationHelper,
    private val listener: RenderListener
) : GLSurfaceView.Renderer {

    private val backgroundRenderer = BackgroundRenderer()
    private val planeRenderer = PlaneRenderer()
    private val pointCloud = PointCloudRenderer()

    private val cube = ObjectRenderer()
    private val cubeSelected = ObjectRenderer()
    private var rectRenderer: RectanglePolygonRenderer? = null

    private var viewWidth = 0
    private var viewHeight = 0

    // according to cube.obj, cube diameter = 0.02f
    private val cubeHitAreaRadius = 0.08f
    private val centerVertexOfCube = floatArrayOf(0f, 0f, 0f, 1f)
    private val vertexResult = FloatArray(4)
    private val projmtx = FloatArray(16)
    private val viewmtx = FloatArray(16)
    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        Logger.logStatus("onSurfaceCreated()")
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        Logger.log(TAG, "onSurfaceCreated")

        // Create the texture and pass it to ARCore session to be filled during update().
        backgroundRenderer.createOnGlThread(context)
        if (session != null) {
            session.setCameraTextureName(backgroundRenderer.getTextureId())
        }

        // Prepare the other rendering objects.
        try {
            rectRenderer = RectanglePolygonRenderer()
            cube.createOnGlThread(context, "cube.obj", "cube_green.png")
            cube.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f)
            cubeSelected.createOnGlThread(context,"cube.obj", "cube_cyan.png")
            cubeSelected.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f)
        } catch (e: IOException) {
            Logger.log(TAG, "Failed to read obj file")
        }
        try {
            planeRenderer.createOnGlThread(context, "trigrid.png")
        } catch (e: IOException) {
            Logger.log(TAG, "Failed to read plane texture")
        }
        pointCloud.createOnGlThread(context)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        if (width <= 0 || height <= 0) {
            Logger.logStatus("onSurfaceChanged(), <= 0")
            return
        }
        Logger.logStatus("onSurfaceChanged()")
        displayRotationHelper.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
        viewWidth = width
        viewHeight = height
    }

    override fun onDrawFrame(gl: GL10) {
        Logger.log(TAG, "onDrawFrame(), width=$viewWidth, height=$viewHeight")
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (viewWidth == 0 || viewWidth == 0) {
            return
        }
        if (session == null) {
            return
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session)
        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId())

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            val frame: Frame = session.update()
            val camera = frame.camera
            // Draw background.
            backgroundRenderer.draw(frame)

            // If not tracking, don't draw 3d objects.
            if (camera.trackingState == TrackingState.PAUSED) {
                return
            }

            // Get projection matrix.
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)

            // Get camera matrix and draw.
            camera.getViewMatrix(viewmtx, 0)

            // Compute lighting from average intensity of the image.
            lightIntensity = frame.lightEstimate.pixelIntensity

            // Visualize tracked points.
            val pointCloud = frame.acquirePointCloud()
            this.pointCloud.update(pointCloud)
            this.pointCloud.draw(viewmtx, projmtx)

            // Application is responsible for releasing the point cloud resources after
            // using it.
            pointCloud.release()

            // Visualize planes.
            planeRenderer.drawPlanes(
                session.getAllTrackables(Plane::class.java), camera.displayOrientedPose, projmtx
            )

            listener.onFrame(this, frame, camera, viewWidth, viewHeight)

        } catch (t: Throwable) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t)
        }

    }

    private var lightIntensity: Float = 0f

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private val anchorMatrix = FloatArray(16)

    private fun drawObject(anchor: Anchor, objectRenderer: ObjectRenderer) {
        anchor.pose.toMatrix(anchorMatrix, 0)
        objectRenderer.updateModelMatrix(anchorMatrix, 1f)
        objectRenderer.draw(viewmtx, projmtx, lightIntensity)
    }

    fun drawCube(anchor: Anchor) = drawObject(anchor, cube)

    fun drawSelectedCube(anchor: Anchor) = drawObject(anchor, cubeSelected)

    fun drawLine(pose0: Pose, pose1: Pose) {
        val lineWidth = 0.002f
        val lineWidthH = lineWidth / viewHeight * viewWidth
        rectRenderer?.setVerts(
            pose0.tx() - lineWidth, pose0.ty() + lineWidthH, pose0.tz() - lineWidth,
            pose0.tx() + lineWidth, pose0.ty() + lineWidthH, pose0.tz() + lineWidth,
            pose1.tx() + lineWidth, pose1.ty() + lineWidthH, pose1.tz() + lineWidth,
            pose1.tx() - lineWidth, pose1.ty() + lineWidthH, pose1.tz() - lineWidth,
            pose0.tx() - lineWidth, pose0.ty() - lineWidthH, pose0.tz() - lineWidth,
            pose0.tx() + lineWidth, pose0.ty() - lineWidthH, pose0.tz() + lineWidth,
            pose1.tx() + lineWidth, pose1.ty() - lineWidthH, pose1.tz() + lineWidth,
            pose1.tx() - lineWidth, pose1.ty() - lineWidthH, pose1.tz() - lineWidth
        )
        rectRenderer?.draw(viewmtx, projmtx)
    }

    fun isHitObject(motionEvent: MotionEvent): Boolean {
        return isMVPMatrixHitMotionEvent(cubeSelected.modelViewProjectionMatrix, motionEvent)
                || isMVPMatrixHitMotionEvent(cube.modelViewProjectionMatrix, motionEvent)
    }

    private fun isMVPMatrixHitMotionEvent(
        ModelViewProjectionMatrix: FloatArray,
        event: MotionEvent?
    ): Boolean {
        if (event == null) {
            return false
        }
        Matrix.multiplyMV(vertexResult, 0, ModelViewProjectionMatrix, 0, centerVertexOfCube, 0)
        /**
         * vertexResult = [x, y, z, w]
         *
         * coordinates in View
         * ┌─────────────────────────────────────────┐╮
         * │[0, 0]                     [viewWidth, 0]│
         * │       [viewWidth/2, viewHeight/2]       │view height
         * │[0, viewHeight]   [viewWidth, viewHeight]│
         * └─────────────────────────────────────────┘╯
         * ╰                view width               ╯
         *
         * coordinates in GLSurfaceView frame
         * ┌─────────────────────────────────────────┐╮
         * │[-1.0,  1.0]                  [1.0,  1.0]│
         * │                 [0, 0]                  │view height
         * │[-1.0, -1.0]                  [1.0, -1.0]│
         * └─────────────────────────────────────────┘╯
         * ╰                view width               ╯
         */
        // circle hit test
        val radius = viewWidth / 2 * (cubeHitAreaRadius / vertexResult[3])
        val dx = event.x - viewWidth / 2 * (1 + vertexResult[0] / vertexResult[3])
        val dy = event.y - viewHeight / 2 * (1 - vertexResult[1] / vertexResult[3])
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble())
        //            // for debug
//            overlayViewForTest.setPoint("cubeCenter", screenX, screenY);
//            overlayViewForTest.postInvalidate();
        return distance < radius
    }

    interface RenderListener {
        fun onFrame(renderer: GLSurfaceRenderer, frame: Frame, camera: Camera, viewWidth: Int, viewHeight: Int)
    }

    companion object {
        private const val TAG = "GLSurfaceRenderer"
    }
}