package br.com.simbasoft.renderer;

public class Matrix4 {
    private double[][] m;

    public Matrix4() {
        m = new double[4][4];

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                m[i][j] = 0;
            }
        }
    }

    public static Matrix4 makePerspective(double fov,
                                          double aspect,
                                          double znear,
                                          double zfar) {
        Matrix4 m = new Matrix4();

        m.m[0][0] = aspect * (1 / Math.tan(fov / 2));
        m.m[1][1] = 1 / Math.tan(fov / 2);
        m.m[2][2] = zfar / (zfar - znear);
        m.m[2][3] = (-zfar * znear) / (zfar - znear);
        m.m[3][2] = 1.0;

        return m;
    }

    public static Matrix4 makeIdentity() {
        Matrix4 m = new Matrix4();

        m.m[0][0] = 1;
        m.m[1][1] = 1;
        m.m[2][2] = 1;
        m.m[3][3] = 1;

        return m;
    }

    public static Matrix4 makeScale(double sx, double sy, double sz) {
        Matrix4 m = makeIdentity();

        m.m[0][0] = sx;
        m.m[1][1] = sy;
        m.m[2][2] = sz;

        return m;
    }

    public static Matrix4 makeRotationX(double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);

        Matrix4 m = makeIdentity();

        m.m[1][1] = c;
        m.m[1][2] = -s;
        m.m[2][1] = s;
        m.m[2][2] = c;

        return m;
    }

    public static Matrix4 makeRotationY(double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);

        Matrix4 m = makeIdentity();

        m.m[0][0] = c;
        m.m[0][2] = s;
        m.m[2][0] = -s;
        m.m[2][2] = c;

        return m;
    }

    public static Matrix4 makeRotationZ(double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);

        Matrix4 m = makeIdentity();

        m.m[0][0] = c;
        m.m[0][1] = -s;
        m.m[1][0] = s;
        m.m[1][1] = c;

        return m;
    }

    public static Matrix4 makeTranslation(double tx, double ty, double tz) {
        Matrix4 m = makeIdentity();

        m.m[0][3] = tx;
        m.m[1][3] = ty;
        m.m[2][3] = tz;

        return m;
    }

    public static Matrix4 multiply(Matrix4 a, Matrix4 b) {
        Matrix4 m = new Matrix4();

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                m.m[i][j] = a.m[i][0] * b.m[0][j] + a.m[i][1] * b.m[1][j] + a.m[i][2] * b.m[2][j] + a.m[i][3] * b.m[3][j];
            }
        }

        return m;
    }

    public static Vector4 multiplyVector4(Matrix4 m, Vector4 v) {
        return new Vector4(
            m.m[0][0] * v.getX() + m.m[0][1] * v.getY() + m.m[0][2] * v.getZ() + m.m[0][3] * v.getW(),
            m.m[1][0] * v.getX() + m.m[1][1] * v.getY() + m.m[1][2] * v.getZ() + m.m[1][3] * v.getW(),
            m.m[2][0] * v.getX() + m.m[2][1] * v.getY() + m.m[2][2] * v.getZ() + m.m[2][3] * v.getW(),
            m.m[3][0] * v.getX() + m.m[3][1] * v.getY() + m.m[3][2] * v.getZ() + m.m[3][3] * v.getW()
        );
    }

    public static Matrix4 lookAt(Vector3 eye, Vector3 target, Vector3 up) {
        Vector3 z = Vector3.normalize(Vector3.subtract(target, eye));
        Vector3 x = Vector3.normalize(Vector3.cross(up, z));
        Vector3 y = Vector3.cross(z, x);

        Matrix4 viewMatrix = new Matrix4();

        viewMatrix.m = new double[][] {
                { x.x(),  x.y(),  x.z(),  -Vector3.dot(x, eye) },
                { y.x(),  y.y(),  y.z(),  -Vector3.dot(y, eye) },
                { z.x(),  z.y(),  z.z(),  -Vector3.dot(z, eye) },
                { 0,         0,         0,         1 }
        };

        return viewMatrix;
    }
}
