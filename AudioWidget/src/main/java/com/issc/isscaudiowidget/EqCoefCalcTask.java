package com.issc.isscaudiowidget;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

public class EqCoefCalcTask  extends AsyncTask<Void, Void, String> {

    EqualizerFragment container;
    private boolean D = true;
    private static final String TAG = "EqCoefCalcTask";

    public EqCoefCalcTask(EqualizerFragment EqFrag)
    {
        this.container = EqFrag;
    }
    public FilterCoeffWrap mFilterCoeff = null;
    public EqCoeffDlg mEqCoeffDlg1 = null;

    public static final int Quanti_LSB = 30;
    public static final int Quanti_MSB = 14;
    public static final int Sim_Bit_Drop = 0;

    protected String doInBackground(Void... arg0) {
        try {

            // Emulate a long running process
            mFilterCoeff =  FilterCoeffWrap.getInstance();
            mEqCoeffDlg1 =  EqCoeffDlg.getInstance();
            mFilterCoeff.initFilterCoeffWrap();
            for(int i=0;i<mEqCoeffDlg1.m_StageNum;i++) {
                mFilterCoeff.boost(i,mEqCoeffDlg1.dBToG(mEqCoeffDlg1.m_Gain[i]),mEqCoeffDlg1.m_Freq[i],mEqCoeffDlg1.m_Q[i],mEqCoeffDlg1.m_SampleFreq);
                mFilterCoeff.FilterFixQuantize(i,(int)Quanti_LSB-Sim_Bit_Drop);
                if (isCancelled()) break;
            }
            mFilterCoeff.glbnormandfixed();

        }catch(Exception ex) {
            if(D) Log.d(TAG, "co-efficient calc exception occurred +",ex);
        }
        return "Coefficient calc finished";
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        container.showProgressBar();
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        // The activity can be null if it is thrown out by Android while task is running!
        if(container!=null && container.getActivity()!=null) {
            container.populateResult(result);
            container.hideProgressBar();
            SharedPreferences Eq_set_dsp_data = container.getActivity().getSharedPreferences("com.issc.isscaudiowidget", 0);
            SharedPreferences.Editor edit = Eq_set_dsp_data.edit();
            for (int i = 0; i < mFilterCoeff.IIR_Coeff_Buff.length; i++) {
                edit.putInt("EqData_"+ Integer.toString(i), mFilterCoeff.IIR_Coeff_Buff[i]);
            }
            edit.commit();
            this.container = null;
        }
    }

    protected void onCancelled()
    {
        super.onCancelled();
    }
}
