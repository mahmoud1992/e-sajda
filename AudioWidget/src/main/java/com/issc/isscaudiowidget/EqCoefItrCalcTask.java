package com.issc.isscaudiowidget;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

public class EqCoefItrCalcTask extends AsyncTask<Void, Void, String> {

    EqualizerFragment container;
    private boolean D = true;
    private static final String TAG = "EqCoefItrCalcTask";

    public EqCoefItrCalcTask(EqualizerFragment EqFrag)
    {
        this.container = EqFrag;
    }
    public FilterCoeffWrap mFilterCoeff = null;
    public EqCoeffDlg mEqCoeffDlg1 = null;

    protected String doInBackground(Void... arg0) {
        try {
            mFilterCoeff =  FilterCoeffWrap.getInstance();
            mEqCoeffDlg1 =  EqCoeffDlg.getInstance();
            mFilterCoeff.initFilterCoeffWrap();

            mFilterCoeff.Itrinit();
            if (isCancelled())
                return "thread exited in the middle";
            //container.UpdateEqGui(); shifted to post execute GUI update portion of code.
            for(int i=0;i<mEqCoeffDlg1.m_StageNum;i++) {
                mFilterCoeff.boost(i,mEqCoeffDlg1.dBToG(mEqCoeffDlg1.m_Gain[i]),mEqCoeffDlg1.m_Freq[i],mEqCoeffDlg1.m_Q[i],mEqCoeffDlg1.m_SampleFreq);
                mFilterCoeff.FilterFixQuantize(i,(int)mEqCoeffDlg1.Quanti_LSB-mEqCoeffDlg1.Sim_Bit_Drop);
                if (isCancelled()) break;
            }

            mFilterCoeff.glbnormandfixed();

        }catch(Exception ex) {
            if(D) Log.d(TAG, "co-efficient Iteration calc exception occurred +", ex);
        }
        return "Coefficient iteration calc finished";
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
            container.UpdateEqGui(0);
            container.populateItrResult(result);
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
        if(mFilterCoeff!=null)
            mFilterCoeff = null;
    }
}
