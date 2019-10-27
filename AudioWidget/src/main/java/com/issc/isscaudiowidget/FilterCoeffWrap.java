package com.issc.isscaudiowidget;

import android.util.Log;

public class FilterCoeffWrap {

    //singleton
    private static FilterCoeffWrap fcinstance = null;
    private boolean D = false;
    private static final String TAG = "FilterCoeffWrap";

    short []IIR_Coeff_Buff= new short[42];
    //constants
    public static final int m_AccuracyNum = 2048*8;
    public static final double ripple = 0.02;
    public static final int Max_Stage = 5;
    public static final int Quanti_LSB = 30;
    public static final int Quanti_MSB = 14;
    //public static final int Log_shift_val = 5;

    protected FilterCoeff[] pFilter = null;
    public EqCoeffDlg mEqCoeffDlg= null;

    double AdpG[],AdpQ[],refF[],refG[];
    double IntG[],BandG[],tmp;
    long NormF[];

    private FilterCoeffWrap()
    {
        // Exists only to defeat instantiation.
    }
    public static FilterCoeffWrap getInstance() {
        if(fcinstance == null) {
            fcinstance = new FilterCoeffWrap();
        }
        return fcinstance;
    }

    public void reset() {
       for(int i = 0;i < Max_Stage;i++) {
           pFilter[i].setStage(0);
           pFilter[i].setGain(0);
           pFilter[i].setDen(0,0);
           pFilter[i].setDen(1,0);
           pFilter[i].setDen(2,0);
           pFilter[i].setNum(0, 0);
           pFilter[i].setNum(1, 0);
           pFilter[i].setNum(2, 0);
       }
    }

    public void initFilterCoeffWrap() {

        pFilter = new FilterCoeff[Max_Stage];
        for(int i=0;i<Max_Stage;i++)
            pFilter[i] = new FilterCoeff();
        reset();
        mEqCoeffDlg =  EqCoeffDlg.getInstance();
    }

    public void Itrinit() {
        int i, j;
        int m_StageNum = mEqCoeffDlg.m_StageNum;
        AdpG = new double[Max_Stage];
        AdpQ = new double[Max_Stage];
        refF = new double[Max_Stage];
        refG = new double[Max_Stage];
        IntG = new double[Max_Stage - 1];
        BandG = new double[Max_Stage - 1];
        NormF = new long[Max_Stage];

        for (j = 0; j < Max_Stage; j++) {
            AdpG[j] = mEqCoeffDlg.dBToG(mEqCoeffDlg.m_Gain[j]);
            AdpQ[j] = mEqCoeffDlg.m_Q[j];
            refF[j] = mEqCoeffDlg.m_Freq[j];
        }

        FilterSort(refF, AdpG, AdpQ, m_StageNum);

        for (j = 0; j < Max_Stage; j++) {
            refG[j] = AdpG[j] * mEqCoeffDlg.dBToG(mEqCoeffDlg.m_GlobalGain);
            mEqCoeffDlg.m_Freq[j] = refF[j];
        }

        for (i = 0; i < m_StageNum; i++) {
            tmp = mEqCoeffDlg.m_SampleFreq - refF[i];
            if (tmp <= 0) {
                if(D) Log.d(TAG , "Filter central frequency incorrect! \n iteration:" + i + "Sampling frequency" + tmp);
                return;
            }
        }

        for (i = 0; i < m_StageNum; i++) {
            NormF[i] = (long) (refF[i] * m_AccuracyNum * 2 / (mEqCoeffDlg.m_SampleFreq));
        }
        for (i = 1; i < m_StageNum; i++) {
            //IntG[i-1]=(refG[i-1]+refG[i])/2;
            tmp = refG[i - 1] * refG[i];
            IntG[i - 1] = Math.sqrt(tmp);
            tmp = (refG[i - 1] + refG[i]) / 2 - 1;
            tmp = Math.abs(tmp) - Math.abs(IntG[i - 1] - 1);
            if (tmp < 0)
                IntG[i - 1] = (refG[i - 1] + refG[i]) / 2;
        }

        double[] ResponseResult = new double[ m_AccuracyNum];
        double[] ResponseTemp = new double[ m_AccuracyNum];

        for (i = 0; i < mEqCoeffDlg.m_IterVal; i++) {
            for (j = 0; j < m_StageNum; j++)
                boost(j, AdpG[j], refF[j], AdpQ[j], mEqCoeffDlg.m_SampleFreq);

            FreqResponse(pFilter[0], m_AccuracyNum, ResponseResult);

            for(int ii=1;ii<m_StageNum;ii++)
            {
                FreqResponse(pFilter[ii],m_AccuracyNum,ResponseTemp);
                for(int jj=0;jj<m_AccuracyNum;jj++)
                {
                    ResponseResult[jj]=ResponseResult[jj]*ResponseTemp[jj];
                }
            }

            for(int jj=0;jj<m_AccuracyNum;jj++)
            {
                ResponseResult[jj]=ResponseResult[jj]*mEqCoeffDlg.dBToG(mEqCoeffDlg.m_GlobalGain);
            }

            for (int jj=0;jj<m_StageNum;jj++)
            {
                AdpG[jj]=AdpG[jj]+(refG[jj]-ResponseResult[(int)NormF[jj]])*.5/mEqCoeffDlg.dBToG(mEqCoeffDlg.m_GlobalGain);
            }

            for (int jj=0;jj<m_StageNum-1;jj++)
            {
                tmp=((int)NormF[jj]+NormF[jj+1])/2;
                BandG[jj]=ResponseResult[(int)tmp];
                if (((refG[jj]>1)&&(refG[jj+1]>1))||((refG[jj]<1)&&(refG[jj+1]<1)))
                {
                    if ((BandG[jj]>ResponseResult[(int)NormF[jj]])&&(BandG[jj]>ResponseResult[(int)NormF[jj+1]]))
                        BandG[jj]=maxVal(ResponseResult,(int)NormF[jj],(int)NormF[jj+1]);
                    else
                    {
                        if ((BandG[jj]<ResponseResult[(int)NormF[jj]])&&(BandG[jj]<ResponseResult[(int)NormF[jj+1]]))
                            BandG[jj]=minVal(ResponseResult,(int)NormF[jj],(int)NormF[jj+1]);
                    }
                }
                BandG[jj]=BandG[jj]/IntG[jj];
                BandG[jj] = Math.log10(BandG[jj]);
                tmp=IntG[jj]*ripple;
                tmp=BandG[jj]-Math.log10(tmp);
                if (tmp<=0)
                    BandG[jj]=0;

                if (IntG[jj]>=1)
                {
                    tmp=BandG[jj]/2;
                    AdpQ[jj]=AdpQ[jj]*Math.exp(tmp);
                    AdpQ[jj+1]=AdpQ[jj+1]*Math.exp(tmp);
                }
                else
                {
                    tmp=BandG[jj]/(-2);
                    AdpQ[jj]=AdpQ[jj]*Math.exp(tmp);
                    AdpQ[jj+1]=AdpQ[jj+1]*Math.exp(tmp);
                }
            }
        }

        for (j=0;j<Max_Stage;j++)
        {
            mEqCoeffDlg.m_Gain[j]= mEqCoeffDlg.GTodB(AdpG[j]);
            mEqCoeffDlg.m_Q[j] = AdpQ[j];
        }
    }

    void FreqResponse(FilterCoeff In,int N,double[] result)
    {
        double realNum=0,imagNum=0,realDen=0,imagDen=0,realRes,imagRes,tmpDen;
        double PI=3.1415926535898,wcT,NwcT;
        //wcT=2*PI/(double)N;

        int i,j;
        for (i=0;i<N;i++)
        {
            realNum=1;
            imagNum=0;
            realDen=1;
            imagDen=0;
            wcT=PI/(double)N*(double)i;

            for (j=1;j<=In.getStage();j++)
            {
                NwcT=(-1)*wcT*(double)j;
                realNum=realNum+In.getNum(j)*Math.cos(NwcT);
                imagNum=imagNum+In.getNum(j)*Math.sin(NwcT);
                realDen=realDen+In.getDen(j)*Math.cos(NwcT);
                imagDen=imagDen+In.getDen(j)*Math.sin(NwcT);
            }

            tmpDen=realDen*realDen+imagDen*imagDen;

            realRes=realNum*realDen+imagNum*imagDen;
            imagRes=realDen*imagNum-imagDen*realNum;
            realRes=realRes/tmpDen;
            imagRes=imagRes/tmpDen;

            tmpDen=realRes*realRes+imagRes*imagRes;
            //result[i]=In->Gain*pow(tmpDen,0.5);
            result[i]=In.getGain()*Math.sqrt(tmpDen);
        }
    }

    public double round(double in)
    {
        double tmp;
        tmp=Math.floor(in);
        in=in-tmp;
        in=in*2;
        if (in>=1)
            tmp++;
        return tmp;
    }

    public double maxVal(double[] in,int head,int tail)
    {
        double tmp;
        tmp=in[tail];
        int i;
        for (i=head;i<tail;i++)
        {
            if (in[i]>tmp)
                tmp=in[i];
        }

        return tmp;
    }

    public double minVal(double []in,int head,int tail)
    {
        double tmp;
        tmp=in[tail];
        int i;
        for (i=head;i<tail;i++)
        {
            if (in[i]<tmp)
                tmp=in[i];
        }

        return tmp;
    }

    void FilterFixQuantize(int index,int Bits)
    {
        int i;
        double tmp;

        for (i=1;i<=pFilter[index].Stage;i++)
        {
            tmp=pFilter[index].Den[i]*Math.pow(2.0,Bits);
            tmp=round(tmp);
            pFilter[index].Den[i]=tmp/Math.pow(2.0,Bits);

            tmp=pFilter[index].Num[i]*Math.pow(2.0,Bits);
            tmp=round(tmp);
            pFilter[index].Num[i]=tmp/Math.pow(2.0,Bits);

        }
    }

    public int boost(int index,double gain,double fc,double Q,double fs)
    {
        double PI=3.1415926535898;
        double wcT,K,V;
        double tempv;

        V=fs/2-fc;
        if(V<=0)
            return 0;

        pFilter[index].Stage=2;
        if (gain==1)
        {
            pFilter[index].setStage(0);
            pFilter[index].setGain(1);
            pFilter[index].setDen(0,1);
            pFilter[index].setNum(0,1);
            pFilter[index].setDen(1,0);
            pFilter[index].setNum(1,0);
            pFilter[index].setDen(2,0);
            pFilter[index].setNum(2,0);
            return 1;
        }

        wcT=2*PI*fc/fs;

        K=Math.tan(wcT/2);
        V=gain;

        pFilter[index].setNum(0,1+V*K/Q+K*K);
        pFilter[index].setNum(1, 2 * (K * K - 1));
        pFilter[index].setNum(2, 1 - V * K / Q + K * K);
        pFilter[index].setDen(0,1 + K/Q + K*K);
        pFilter[index].setDen(1, 2 * (K * K - 1));
        pFilter[index].setDen(2,1 - K/Q + K*K);

        V=pFilter[index].getNum(0);
        K=pFilter[index].getDen(0);
        for (int i=0;i<3;i++)
        {

            tempv=pFilter[index].getNum(i)/V;
            pFilter[index].setNum(i,tempv);
            tempv=pFilter[index].getDen(i)/K;
            pFilter[index].setDen(i,tempv);
        }

        pFilter[index].setGain(V/K);

        return 1;
    }

    public void FilterSort2(double []Lead,FilterCoeff[] sub1,long N)
    {
        //Bubble minmax sort
        //drop gain=1 filter button to remove redundant filter
        int i,j;
        double tmp;
        FilterCoeff tmp2;
        for (i=0;i<N-1;i++)
        {
            for (j=0;j<N-1;j++)
            {

                if (((Lead[j]>Lead[j+1])||(Lead[j]==1))&&(Lead[j+1]!=1))
                {
                    tmp=Lead[j];
                    Lead[j]=Lead[j+1];
                    Lead[j+1]=tmp;

                    tmp2=sub1[j];
                    sub1[j]=sub1[j+1];
                    sub1[j+1]=tmp2;
                }

            }

        }

    }

    void FilterSort(double []Lead,double []sub1,double []sub2,long N)
    {
        //Bubble minmax sort
        int i,j;
        double tmp;
        for (i=0;i<N-1;i++)
        {
            for (j=0;j<N-1;j++)
            {

                if (Lead[j]>Lead[j+1])
                {
                    tmp=Lead[j];
                    Lead[j]=Lead[j+1];
                    Lead[j+1]=tmp;

                    tmp=sub1[j];
                    sub1[j]=sub1[j+1];
                    sub1[j+1]=tmp;

                    tmp=sub2[j];
                    sub2[j]=sub2[j+1];
                    sub2[j+1]=tmp;

                }

            }

        }

    }

    public int glbnormandfixed()
    {
        double GGain,tmp;
        double tmpG[] = new double[Max_Stage];
        FilterCoeff []tmpFilter = new FilterCoeff[Max_Stage];

        long MSB;
        long LSB;
        int i ,j;
        GGain = mEqCoeffDlg.dBToG(mEqCoeffDlg.m_GlobalGain);

        for(i=0; i<mEqCoeffDlg.m_StageNum;i++) {

            tmpFilter[i]=pFilter[i];
            tmpG[i]=mEqCoeffDlg.dBToG(mEqCoeffDlg.m_Gain[i]);

        }

        FilterSort2(tmpG,tmpFilter,mEqCoeffDlg.m_StageNum);
        short count = 0 ;

        //Need to sort the output sequence of filters by the value of gains
        //Output all coefficients as unsigned mode
        for(i=0;i<mEqCoeffDlg.m_StageNum;i++) {
            for (j = 2; j > 0; j--) {
                //tmp=pow(2.0,Quanti_MSB)*pFilter[i]->Num[j];
                tmp = Math.pow(2.0, Quanti_MSB) * (tmpFilter[i].getNum(j));
                MSB = (long) Math.floor(tmp);
                LSB = (long) ((tmp - (double) MSB) * Math.pow(2.0, 16));
                if (MSB < 0)
                    MSB = MSB + (long) Math.pow(2.0, 16);

                if(D)  Log.d(TAG, "count:" + count);

                IIR_Coeff_Buff[count++] = (short)MSB;
                IIR_Coeff_Buff[count++] = (short)LSB;

                if(D)  Log.d(TAG, String.format("%d", MSB));
                if(D)  Log.d(TAG, String.format("%d", LSB));
            }

            for (j=2;j>0;j--)
            {
                //tmp=pow(2.0,Quanti_MSB)*pFilter[i]->Den[j];
                tmp=Math.pow(2.0,Quanti_MSB)*tmpFilter[i].getDen(j);
                MSB =(long)Math.floor(tmp);
                LSB =(long)((tmp-(double)MSB)*Math.pow(2.0,16));
                if (MSB<0)
                    MSB=(long)(MSB+Math.pow(2.0,16));

                if(D)  Log.d(TAG, "count:" + count);

                IIR_Coeff_Buff[count++]=(short)MSB;
                IIR_Coeff_Buff[count++]=(short)LSB;

                if(D)  Log.d(TAG, String.format("%d", MSB));
                if(D)  Log.d(TAG, String.format("%d", LSB));
            }
            GGain = GGain*pFilter[i].getGain();
        }

        for (i=mEqCoeffDlg.m_StageNum;i<Max_Stage;i++)
        {
            IIR_Coeff_Buff[count++]=0;  IIR_Coeff_Buff[count++]=0;  IIR_Coeff_Buff[count++]=0;  IIR_Coeff_Buff[count++]=0;
            IIR_Coeff_Buff[count++]=0;  IIR_Coeff_Buff[count++]=0;  IIR_Coeff_Buff[count++]=0;  IIR_Coeff_Buff[count++]=0;
        }

        GGain=Math.pow(2.0,Quanti_LSB)*GGain;
        GGain=round(GGain);
        GGain=GGain/Math.pow(2.0,Quanti_LSB);

        tmp=Math.pow(2.0,Quanti_MSB)*GGain;
        MSB=(long)Math.floor(tmp);
        LSB=(long)((tmp-(double)MSB)*Math.pow(2.0, 16));
        if (MSB<0)
            MSB=(long)(MSB+Math.pow(2.0,16));

        if(D)  Log.d(TAG, "count:" + count);

        IIR_Coeff_Buff[count++]=(short)MSB;
        IIR_Coeff_Buff[count]=(short)LSB;

        if(D)  Log.d(TAG, String.format("%d", MSB));
        if(D)  Log.d(TAG, String.format("%d", LSB));

        return 1;
    }

}
