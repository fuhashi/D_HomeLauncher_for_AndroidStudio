package jp.co.disney.apps.managed.kisekaeapp.system.view;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

public class ViewUtils {

    private static float buggyMatrixValues[];
    private static Matrix tmpMatrix = new Matrix();
    private static float tmpPtsF[] = new float[2];
    private static RectF tmpRectF = new RectF();

    public static boolean getMatrixToParent(View view, View view1, Matrix matrix)
    {
        matrix.reset();
        boolean flag = true;
        do
        {
            int i;
            int j;
            if(view == null || view == view1)
                if(view != view1)
                    throw new IllegalArgumentException();
                else
                    return flag;
            i = view.getScrollX();
            j = view.getScrollY();
            if(i != 0 || j != 0)
            {
                matrix.postTranslate(-i, -j);
                flag = false;
            }
            if(!view.getMatrix().isIdentity())
            {
                matrix.postConcat(view.getMatrix());
                flag = false;
            }
            i = view.getLeft();
            j = view.getTop();
            if(i != 0 || j != 0)
            {
                matrix.postTranslate(view.getLeft(), view.getTop());
                flag = false;
            }
            if(view.getParent() instanceof View)
                view = (View)view.getParent();
            else
                view = null;
        } while(true);
    }

    public static void matrixMapRect(Matrix matrix, boolean flag, RectF rectf)
    {
        if(!flag && matrix.isIdentity())
        {
            if(buggyMatrixValues == null)
                buggyMatrixValues = new float[9];
            matrix.getValues(buggyMatrixValues);
            tmpPtsF[0] = rectf.left;
            tmpPtsF[1] = rectf.top;
            buggyMatrixMapPoints(tmpPtsF);
            rectf.left = tmpPtsF[0];
            rectf.top = tmpPtsF[1];
            tmpPtsF[0] = rectf.right;
            tmpPtsF[1] = rectf.bottom;
            buggyMatrixMapPoints(tmpPtsF);
            rectf.right = tmpPtsF[0];
            rectf.bottom = tmpPtsF[1];
            return;
        } else
        {
            matrix.mapRect(rectf);
            return;
        }
    }

    public static void getRectInParent(View view, View view1, Rect rect)
    {
        Matrix matrix = tmpMatrix;
        boolean flag = getMatrixToParent(view, view1, matrix);
        tmpRectF.set(rect);
        matrixMapRect(matrix, flag, tmpRectF);
        rect.set((int)tmpRectF.left, (int)tmpRectF.top, (int)tmpRectF.right, (int)tmpRectF.bottom);
    }

    private static void buggyMatrixMapPoints(float af[])
    {
        float f = (int)af[0];
        float f1 = (int)af[1];
        float f2 = buggyMatrixValues[6] * f + buggyMatrixValues[7] * f1 + buggyMatrixValues[8];
        af[0] = buggyMatrixValues[0] * f + buggyMatrixValues[1] * f1 + buggyMatrixValues[2];
        af[1] = buggyMatrixValues[3] * f + buggyMatrixValues[4] * f1 + buggyMatrixValues[5];
        af[0] = af[0] / f2;
        af[1] = af[1] / f2;
    }
}
