package com.issc.isscaudiowidget;

import static java.lang.Math.log10;
import static java.lang.Math.pow;

public class EqCoeffDlg {
    //singleton
    private static EqCoeffDlg instance = null;

    private static final boolean DB_GAIN = true;
    public static final double ripple = 0.02;
    public static final double Max_dB  =  12;
    public static final double Min_dB  = 40;
    public static final int Quanti_LSB = 30;
    public static final int Quanti_MSB = 14;
    public static final int Log_shift_val =  5;
    public static final int Max_Stage = 5;
    public static final double Max_gaindb = 20;
    public static final double Min_gaindb = -100;
    public static final double Max_Q = 1000;
    public static final double Min_Q = 0;
    public static final int Sim_Bit_Drop = 0 ; //Use full 32 bits

    public double	m_Freq[] = new double[5];
    public double	m_Gain[] = new double[5];;
    public double	m_Q[] = new double[5];;
    public double	m_SampleFreq;
    public double	m_GlobalGain;
    public int		m_IterVal;
    public boolean	m_LogScale;
    public long	m_MaxdB;
    public long	m_MindB;
    public double mGTodB;
    public double mdBToG;
    protected int m_StageNum;
    protected int m_DefaultNum;


    private EqCoeffDlg()
    {
        // Exists only to defeat instantiation.
    }
    public static EqCoeffDlg getInstance() {
        if(instance == null) {
            instance = new EqCoeffDlg();
        }
        return instance;
    }

    public double GTodB ( double in){
        mGTodB = 20.0 * log10(in);
        return mGTodB;
    }

    public double dBToG(double in) {
       mdBToG = pow(10.0, in / (20.0));
        return mdBToG;
    }

    public void UpdateEqBandsData(int position)
    {
        switch (position)
        {
            default:
            case 0: //Flat
                m_Freq[0] = 1000.0;
                m_Freq[1] = 2000.0;
                m_Freq[2] = 4000.0;
                m_Freq[3] = 8000.0;
                m_Freq[4] = 16000.0;
                m_Gain[0] = GTodB(1.0);
                m_Gain[1] = GTodB(1.0);
                m_Gain[2] = GTodB(1.0);
                m_Gain[3] = GTodB(1.0);
                m_Gain[4] = GTodB(1.0);
                m_Q[0] = 1.0;
                m_Q[1] = 1.0;
                m_Q[2] = 1.0;
                m_Q[3] = 1.0;
                m_Q[4] = 1.0;
                m_SampleFreq = 48000.0;
                m_StageNum = 5;
                m_GlobalGain = GTodB(1.0);
                break;
            case 1: //Boost
                m_Freq[0] = 32.0;
                m_Freq[1] = 250.0;
                m_Freq[2] = 1000.0;
                m_Freq[3] = 8000.0;
                m_Freq[4] = 16000.0;
                m_Gain[0] = GTodB(1.92866228512964);
                m_Gain[1] = GTodB(1.31707666338300);
                m_Gain[2] = GTodB(0.89032258594553);
                m_Gain[3] = GTodB(1.0);
                m_Gain[4] = GTodB(1.0);
                m_Q[0] = 0.43293644239969;
                m_Q[1] = 0.35638544866886;
                m_Q[2] = 0.74086372129852;
                m_Q[3] = 1.0;
                m_Q[4] = 1.0;
                m_SampleFreq = 48000.0;
                m_StageNum = 3;
                m_GlobalGain = GTodB(1);
                break;
            case 2://Treble
                m_Freq[0] = 500.0;
                m_Freq[1] = 1000.0;
                m_Freq[2] = 4000.0;
                m_Freq[3] = 16000.0;
                m_Freq[4] = 20000.0;
                m_Gain[0] = GTodB(0.98658758766945);
                m_Gain[1] = GTodB(1.00878203417550);
                m_Gain[2] = GTodB(1.29198710848956);
                m_Gain[3] = GTodB(1.96821721203325);
                m_Gain[4] = GTodB(1.0);
                m_Q[0] = 1.01016346823979;
                m_Q[1] = 1.15128009113710;
                m_Q[2] = 0.49213181332384;
                m_Q[3] = 0.49349647103298;
                m_Q[4] = 1.0;
                m_SampleFreq = 48000.0;
                m_StageNum = 4;
                m_GlobalGain = GTodB(1);
                break;
            case 3://Pop
                m_Freq[0] = 32.0;
                m_Freq[1] = 125.0;
                m_Freq[2] = 750.0;
                m_Freq[3] = 4000.0;
                m_Freq[4] = 16000.0;
                m_Gain[0] = GTodB(0.98856685820696);
                m_Gain[1] = GTodB(1.09005980510416);
                m_Gain[2] = GTodB(1.75408420359879);
                m_Gain[3] = GTodB(1.09005980510416);
                m_Gain[4] = GTodB(0.98691533400276);
                m_Q[0] = 0.98620270998901;
                m_Q[1] = 0.41931697684387;
                m_Q[2] = 0.58185820541141;
                m_Q[3] = 0.41931697684387;
                m_Q[4] = 0.80454192483280;
                m_SampleFreq = 48000.0;
                m_StageNum = 5;
                m_GlobalGain = GTodB(0.84);
                break;
            case 4://Rock
                m_Freq[0] = 32.0;
                m_Freq[1] = 250.0;
                m_Freq[2] = 1000.0;
                m_Freq[3] = 4000.0;
                m_Freq[4] = 16000.0;
                m_Gain[0] = GTodB(1.97695226195262);
                m_Gain[1] = GTodB(1.08916167643889);
                m_Gain[2] = GTodB(0.84955461886004);
                m_Gain[3] = GTodB(1.05051608753516);
                m_Gain[4] = GTodB(1.99446106397164);
                m_Q[0] = 0.40267199696855;
                m_Q[1] = 0.31218212846435;
                m_Q[2] = 0.32220330969738;
                m_Q[3] = 0.61265796514266;
                m_Q[4] = 0.36854006186122;
                m_SampleFreq = 48000.0;
                m_StageNum = 5;
                m_GlobalGain = GTodB(1);
                break;
            case 5://Classic
                m_Freq[0] = 250.0;
                m_Freq[1] = 750.0;
                m_Freq[2] = 16000.0;
                m_Freq[3] = 20000.0;
                m_Freq[4] = 22000.0;
                m_Gain[0] = GTodB(1.25308149309474);
                m_Gain[1] = GTodB(0.42952440917061);
                m_Gain[2] = GTodB(1.00627225160375);
                m_Gain[3] = GTodB(1.0);
                m_Gain[4] = GTodB(1.0);
                m_Q[0] = 1.84047455179705;
                m_Q[1] = 0.21370992368501;
                m_Q[2] = 0.11611675014813;
                m_Q[3] = 1.0;
                m_Q[4] = 1.0;
                m_SampleFreq = 48000.0;
                m_StageNum = 3;
                m_GlobalGain = GTodB(1.68);
                break;
            case 6://Jazz
                m_Freq[0] = 125.0;
                m_Freq[1] = 250.0;
                m_Freq[2] = 750.0;
                m_Freq[3] = 16000.0;
                m_Freq[4] = 20000.0;
                m_Gain[0] = GTodB(0.80807222401813);
                m_Gain[1] = GTodB(1.19993741976506);
                m_Gain[2] = GTodB(0.51932714953227);
                m_Gain[3] = GTodB(1.00345679502982);
                m_Gain[4] = GTodB(1);
                m_Q[0] = 0.86264758926996;
                m_Q[1] = 1.13540466707967;
                m_Q[2] = 0.27313980983445;
                m_Q[3] = 0.18446552753420;
                m_Q[4] = 1.0;
                m_SampleFreq = 48000.0;
                m_StageNum = 4;
                m_GlobalGain = GTodB(1.585);
                break;
            case 7://Dance
                m_Freq[0] = 64.0;
                m_Freq[1] = 250.0;
                m_Freq[2] = 2000.0;
                m_Freq[3] = 16000.0;
                m_Freq[4] = 20000.0;
                m_Gain[0] = GTodB(2.05339743428121);
                m_Gain[1] = GTodB(0.74175797823560);
                m_Gain[2] = GTodB(1.68327534931948);
                m_Gain[3] = GTodB(0.98185132489032);
                m_Gain[4] = GTodB(1.0);
                m_Q[0] = 0.51647294364063;
                m_Q[1] = 0.69485573711835;
                m_Q[2] = 0.51283185235765;
                m_Q[3] = 0.38117808090389;
                m_Q[4] = 1.0;
                m_SampleFreq = 48000.0;
                m_StageNum = 4;
                m_GlobalGain = GTodB(1.0);
                break;
            case 8://R&B
                m_Freq[0] = 64.0;
                m_Freq[1] = 500.0;
                m_Freq[2] = 4000.0;
                m_Freq[3] = 16000.0;
                m_Freq[4] = 20000.0;
                m_Gain[0] = GTodB(1.79448846234098);
                m_Gain[1] = GTodB(0.55980089306381);
                m_Gain[2] = GTodB(1.00717135310177);
                m_Gain[3] = GTodB(1.10121648869244);
                m_Gain[4] = GTodB(1.06542494612799);
                m_Q[0] = 2.88385310493725;
                m_Q[1] = 0.68953348916826;
                m_Q[2] = 0.20725350274938;
                m_Q[3] = 0.88950806827823;
                m_Q[4] = 1.02619582010820;
                m_SampleFreq = 48000.0;
                m_StageNum = 5;
                m_GlobalGain = GTodB(1.0);
                break;

        }
        m_IterVal = 20;
    }

}
