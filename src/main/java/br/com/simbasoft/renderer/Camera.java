package br.com.simbasoft.renderer;

public class Camera {
    private Vector3 position;
    private Vector3 direction;
    private Vector3 forwardVelocity;
    private double yaw;
    private double pitch;

    public Camera(Vector3 position, Vector3 direction) {
        this.position = position;
        this.direction = direction;
        this.forwardVelocity = new Vector3(0, 0, 0);
        this.yaw = 0;
        this.pitch = 0;
    }

    public Vector3 getLookAtTarget() {
        Vector3 target = new Vector3(0, 0, 1);

        Matrix4 cameraYawRotation = Matrix4.makeRotationY(yaw);
        Matrix4 cameraPitchRotation = Matrix4.makeRotationX(pitch);

        Matrix4 cameraRotation = Matrix4.makeIdentity();
        cameraRotation = Matrix4.multiply(cameraYawRotation, cameraRotation);
        cameraRotation = Matrix4.multiply(cameraPitchRotation, cameraRotation);

        Vector4 cameraDirection = Matrix4.multiplyVector4(cameraRotation, Vector4.fromVector3(target));
        direction = Vector3.fromVector4(cameraDirection);

        target = Vector3.add(position, direction);

        return target;
    }

    public void updateCameraPosition(Vector3 position) {
        this.position = position;
    }

    public void updateCameraForwardVelocity(Vector3 forwardVelocity) {
        this.forwardVelocity = forwardVelocity;
    }

    public void rotateCameraYaw(double angle) {
        this.yaw += angle;
    }

    public void rotateCameraPitch(double angle) {
        this.pitch += angle;
    }

    public Vector3 getPosition() {
        return position;
    }

    public Vector3 getDirection() {
        return direction;
    }

    public Vector3 getForwardVelocity() {
        return forwardVelocity;
    }
}
