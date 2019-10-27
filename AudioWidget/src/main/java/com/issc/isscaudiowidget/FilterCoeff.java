package com.issc.isscaudiowidget;

public class FilterCoeff {
    //Main variables of this class
    int Stage;
    double Gain;
    double Num[] = new double[3];
    double Den[] = new double[3];

    public FilterCoeff()
    {
        Stage = 0;
        Gain = 0;
        Num[0] = 0;
        Num[1] = 0;
        Num[2] = 0;
        Den[0] = 0;
        Den[1] = 0;
        Den[1] = 0;
    }

    public int getStage()
    {
        return Stage;
    }

    public double getGain()
    {
        return Gain;
    }

    public double getNum(int index)
    {
        return Num[index];
    }

    public double getDen(int index)
    {
        return Den[index];
    }


    public void setStage(int mStage)
    {
        Stage = mStage;
    }

    public void setGain(double mgain)
    {
        Gain = mgain;
    }

    public void setNum(int index,double value)
    {
        Num[index] = value;
    }

    public void setDen(int index,double value)
    {
        Den[index] = value;
    }

}
