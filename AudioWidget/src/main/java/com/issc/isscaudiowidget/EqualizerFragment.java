package com.issc.isscaudiowidget;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;


public class EqualizerFragment extends Fragment {

        private Activity mActivity = null;
        private static final String TAG = "EqualizerFragment";
        private boolean D = true;
        private boolean isEqInit = true;

        public Spinner mPresetmodeSpinner = null;
        public Spinner mPresetStageSpinner = null;
        public EqCoeffDlg mEqCoeffDlg= null;
        public int Eqbandmode = -1;
        static boolean isEqsetRunning = false;
        static boolean isEqIterationRunning  = false;
        EqCoefCalcTask mEqCoefCalcTask;
        EqCoefItrCalcTask mEqCoefItrCalcTask;
        String mResult = null,mItrResult = null;
        View mRelatvieLayout;
        public int Eqbandpos;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            if(D) Log.d(TAG, "EqualizerFragment --> onCreate");

            super.onCreate(savedInstanceState);
            mActivity = getActivity();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setRetainInstance(true);
            mEqCoeffDlg =  EqCoeffDlg.getInstance();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            if(D) Log.d(TAG, "EqualizerFragment --> onCreateView");
            mRelatvieLayout = inflater.inflate(
                    R.layout.eq_fragement, container, false);

            return mRelatvieLayout;
        }

        @Override
        public void onStart()
        {
           super.onStart();
            if(isEqInit) {
                mPresetmodeSpinner = (Spinner) mActivity.findViewById(R.id.EQ_PRESET_SPINNER);
                // Create an ArrayAdapter using the string array and a default spinner layout
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mActivity,
                        R.array.EQPRESET_array, R.layout.simple_spinner_item);

                adapter.setDropDownViewResource(R.layout.simlple_spinner_dropdown_item);
                mPresetmodeSpinner.setAdapter(adapter);
                SharedPreferences msp;
                msp = mActivity.getSharedPreferences(mActivity.getPackageName(), 0);
                Eqbandpos = msp.getInt("EqGui_last_bandmode", -1);
                if(Eqbandpos == -1)
                {
                    mEqCoeffDlg.UpdateEqBandsData(Eqbandpos+1);// for first time Application case, set it zero
                }
                mPresetmodeSpinner.setSelection(Eqbandpos);
                mPresetmodeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int position, long id) {

                        mEqCoeffDlg.UpdateEqBandsData(position);
                        Eqbandmode = position;
                        if((Eqbandpos != -1 )&& (isEqInit)) {
                            EqLastStateRestore();
                            SharedPreferences msp;
                            msp = mActivity.getSharedPreferences(mActivity.getPackageName(), 0);
                            Editor editor = msp.edit();
                            editor.putInt("EqGui_last_bandmode", Eqbandmode);
                            editor.commit();
                        }

                        if(mPresetStageSpinner == null) {
                            mPresetStageSpinner = (Spinner) mActivity.findViewById(R.id.EQ_STAGE_SPINNER);
                        }
                        int stagecount = mEqCoeffDlg.m_StageNum-1;
                        mPresetStageSpinner.setSelection(stagecount);
                        UpdateEqGui(0);
                        if (isEqInit == true)
                            isEqInit = false;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        return;
                    }
                });
            }

            //stage spinner update
            if(mPresetStageSpinner == null)
            {
                mPresetStageSpinner = (Spinner) mActivity.findViewById(R.id.EQ_STAGE_SPINNER);
            }
            ArrayAdapter<CharSequence> stageadapter = ArrayAdapter.createFromResource(mActivity,
                    R.array.EQSTAGE_array, R.layout.simple_spinner_item);
            stageadapter.setDropDownViewResource(R.layout.simlple_spinner_dropdown_item);
            mPresetStageSpinner.setAdapter(stageadapter);
            mPresetStageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {

                    if (!isEqInit) {
                        if (mEqCoeffDlg.m_StageNum != (position + 1)) {
                            UpdateEqStageGui(position + 1);
                            mEqCoeffDlg.m_StageNum = position + 1;
                        }
                    }
                    return;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    return;
                }
            });


            //Eq set Button Main functionality
            Button EqSetbtn = (Button) mActivity.findViewById(R.id.Eq_set);
            EqSetbtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Perform action on click
                    boolean datavalid = DoDataValidate();
                    if (datavalid == false) {
                        Toast.makeText(mActivity.getBaseContext(), "data is invalid", Toast.LENGTH_SHORT).show();
                    } else {
                        if (isEqsetRunning == false && isEqIterationRunning == false) {
                            isEqsetRunning = true;
                            startnewEqCalc();
                        } else {
                            Toast.makeText(mActivity.getBaseContext(), "thread is in running state", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });


            //Iteration Button Main functionality
            Button EqIterationbtn = (Button) mActivity.findViewById(R.id.Eq_iteration);
            EqIterationbtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Perform action on click
                    boolean datavalid1 = DoDataValidate();
                    if(datavalid1 == false) {
                        Toast.makeText(mActivity.getBaseContext(), "data is invalid", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        if(isEqIterationRunning == false && isEqsetRunning == false) {
                            isEqIterationRunning =  true;
                            startnewEqIterationCalc();
                        }
                        else {
                            Toast.makeText(mActivity.getBaseContext(), "thread is in running state", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

            //Reset Button functionality
            Button EqReSetbtn = (Button) mActivity.findViewById(R.id.Eq_reset);
            EqReSetbtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mEqCoeffDlg.UpdateEqBandsData(Eqbandmode);
                    if(mPresetStageSpinner == null) {
                        mPresetStageSpinner = (Spinner) mActivity.findViewById(R.id.EQ_STAGE_SPINNER);
                    }
                    int stagecount = mEqCoeffDlg.m_StageNum-1;
                    mPresetStageSpinner.setSelection(stagecount);
                    UpdateEqGui(1);
                }
            });

            //Exit Button functionality handled by default with back button of Android.
            /*Button EqExitbtn = (Button) mActivity.findViewById(R.id.Eq_Exit);
            EqExitbtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onDestroy();
                }
            });*/
        }

    protected void startnewEqCalc() {

        mEqCoefCalcTask = new EqCoefCalcTask(this);
        mEqCoefCalcTask.execute();
    }

    protected void startnewEqIterationCalc() {

        mEqCoefItrCalcTask = new EqCoefItrCalcTask(this);
        mEqCoefItrCalcTask.execute();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
    // Sync UI state to current fragment and task state

        if(isTaskRunning(mEqCoefCalcTask) || isTaskItrRunning(mEqCoefItrCalcTask)) {
            showProgressBar();
        }else {
            hideProgressBar();
        }

        if(mResult!=null) {
            populateResult(mResult);
        }

        if(mItrResult!=null) {
            populateItrResult(mItrResult);
        }

        super.onActivityCreated(savedInstanceState);
    }

    public void showProgressBar() {
        ProgressBar progress = (ProgressBar)getActivity().findViewById(R.id.progressBarFetch);
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);
    }

    public void hideProgressBar() {
        ProgressBar progress = (ProgressBar)getActivity().findViewById(R.id.progressBarFetch);
        progress.setVisibility(View.GONE);
    }

    public void populateResult(String s) {
        Toast.makeText(mActivity.getBaseContext(),s,Toast.LENGTH_SHORT).show();
        isEqsetRunning = false;

        if ( !((Bluetooth_Conn) mActivity.getApplication()).isSendEqDataThread() &&
                ((Bluetooth_Conn) mActivity.getApplication()).getSppStatus() )
        {
            ((Bluetooth_Conn) mActivity.getApplication()).startSendEqData();
            if(((Bluetooth_Conn) mActivity.getApplication()).isEqDataready() == false)
                ((Bluetooth_Conn) mActivity.getApplication()).SetEqDataready(true);
        } else {
            if (D) Log.d("Equalizer Data", "[Main] isSending or isTransferring");
        }

    }

    public void populateItrResult(String s) {
        Toast.makeText(mActivity.getBaseContext(),s,Toast.LENGTH_SHORT).show();
        isEqIterationRunning = false;
        if ( !((Bluetooth_Conn) mActivity.getApplication()).isSendEqDataThread() &&
                ((Bluetooth_Conn) mActivity.getApplication()).getSppStatus() )
        {
            ((Bluetooth_Conn) mActivity.getApplication()).startSendEqData();
            if(((Bluetooth_Conn) mActivity.getApplication()).isEqDataready() == false)
                ((Bluetooth_Conn) mActivity.getApplication()).SetEqDataready(true);
        } else {
            if (D) Log.d("Equalizer Data", "[Main] isSending or isTransferring");
        }
    }

    protected boolean isTaskRunning(EqCoefCalcTask task) {
        if(task==null ) {
            return false;
        } else if(task.getStatus() == EqCoefCalcTask.Status.FINISHED){
            return false;
        } else {
            return true;
        }
    }

    protected boolean isTaskItrRunning(EqCoefItrCalcTask task) {
        if((task==null) || ((task.getStatus() == EqCoefCalcTask.Status.FINISHED))) {
            return false;
        } /*else if(task.getStatus() == EqCoefCalcTask.Status.FINISHED){
            return false;
        } */
        else {
            return true;
        }
    }

    @Override
    public void onPause() {
        if(D) Log.d(TAG, "EqualizerFragment --> Pause");
        super.onPause();
    }

    @Override
    public void onResume() {
        if(D) Log.d(TAG, "EqualizerFragment --> onResume");
        super.onResume();
    }

    @Override
    public void onStop() {
        if(D) Log.d(TAG, "EqualizerFragment --> onStop");
        super.onStop();
    }

    public void onDestroyView()
    {
        EqLastStatePreserve();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if(D) Log.d(TAG, "EqualizerFragment --> onDestroy");
        super.onDestroy();

        //Release and reset required items
        isEqInit = true;
        if(mEqCoeffDlg != null)
            mEqCoeffDlg = null;
        if(isTaskRunning(mEqCoefCalcTask) ) {
            mEqCoefCalcTask.cancel(true);
        }

        isEqsetRunning = false;
        mEqCoefCalcTask = null;

        if(isTaskItrRunning(mEqCoefItrCalcTask))
            mEqCoefItrCalcTask.cancel(true);
        isEqIterationRunning  = false;
        mEqCoefItrCalcTask = null;

        mResult = null;
        mItrResult = null;
        //onDestroyView();
    }

    public void UpdateEqGui(int rstMode)
    {
        EditText dummyEditText;
        int mstages = mEqCoeffDlg.m_StageNum;

        for(int i=0;i<mstages;i++)
        {
            String lbandfc = "bandfc" + String.valueOf(i);
            //int resID = getResources().getIdentifier(lbandfc, "id", "com.issc.isscaudiowidget");
            int resID = getResources().getIdentifier(lbandfc, "id", mActivity.getPackageName());
            dummyEditText = (EditText) mActivity.findViewById(resID);
            dummyEditText.setText(Double.toString(mEqCoeffDlg.m_Freq[i]));
            dummyEditText.setTextColor(Color.parseColor("#000000"));
            dummyEditText.setFocusable(true);
            dummyEditText.setFocusableInTouchMode(true);
            if(rstMode == 1)
                dummyEditText.setError(null);

            String lbandgain = "bandgain" + String.valueOf(i);
            resID = getResources().getIdentifier(lbandgain, "id", mActivity.getPackageName());
            dummyEditText = (EditText) mActivity.findViewById(resID);
            dummyEditText.setText(Double.toString(mEqCoeffDlg.m_Gain[i]));
            dummyEditText.setTextColor(Color.parseColor("#000000"));
            dummyEditText.setFocusable(true);
            dummyEditText.setFocusableInTouchMode(true);
            if(rstMode == 1)
                dummyEditText.setError(null);

            String lbandQ = "bandQ" + String.valueOf(i);
            resID = getResources().getIdentifier(lbandQ, "id", mActivity.getPackageName());
            dummyEditText = (EditText) mActivity.findViewById(resID);
            dummyEditText.setText(Double.toString(mEqCoeffDlg.m_Q[i]));
            dummyEditText.setTextColor(Color.parseColor("#000000"));
            dummyEditText.setFocusable(true);
            dummyEditText.setFocusableInTouchMode(true);
            if(rstMode == 1)
                dummyEditText.setError(null);
        }

        for(int i=mstages;i<mEqCoeffDlg.Max_Stage;i++)
        {
            String lbandfc = "bandfc" + String.valueOf(i);
            int resID = getResources().getIdentifier(lbandfc, "id", mActivity.getPackageName());
            dummyEditText = (EditText) mActivity.findViewById(resID);
            dummyEditText.setText(Double.toString(mEqCoeffDlg.m_Freq[i]));
            dummyEditText.setFocusable(false);
            dummyEditText.setFocusableInTouchMode(false);
            dummyEditText.setTextColor(Color.parseColor("#ababab"));

            String lbandgain = "bandgain" + String.valueOf(i);
            resID = getResources().getIdentifier(lbandgain, "id", mActivity.getPackageName());
            dummyEditText = (EditText) mActivity.findViewById(resID);
            dummyEditText.setText(Double.toString(mEqCoeffDlg.m_Gain[i]));
            dummyEditText.setFocusable(false);
            dummyEditText.setFocusableInTouchMode(false);
            dummyEditText.setTextColor(Color.parseColor("#ababab"));

            String lbandQ = "bandQ" + String.valueOf(i);
            resID = getResources().getIdentifier(lbandQ, "id",mActivity.getPackageName());
            dummyEditText = (EditText) mActivity.findViewById(resID);
            dummyEditText.setText(Double.toString(mEqCoeffDlg.m_Q[i]));
            dummyEditText.setFocusable(false);
            dummyEditText.setFocusableInTouchMode(false);
            dummyEditText.setTextColor(Color.parseColor("#ababab"));
        }

        dummyEditText = (EditText) mActivity.findViewById(R.id.eqGlgain);
        dummyEditText.setText(Double.toString(mEqCoeffDlg.m_GlobalGain));
        if(rstMode == 1)
            dummyEditText.setError(null);

        dummyEditText = (EditText) mActivity.findViewById(R.id.eqfs);
        dummyEditText.setText(Double.toString(mEqCoeffDlg.m_SampleFreq));
        if(rstMode == 1)
            dummyEditText.setError(null);

        dummyEditText = (EditText) mActivity.findViewById(R.id.Eq_iteration_val);
        dummyEditText.setText(Integer.toString(mEqCoeffDlg.m_IterVal));
        if(rstMode == 1)
            dummyEditText.setError(null);
    }

    public void UpdateEqStageGui(int position) {

        EditText dummyEditText;

        for (int i = 0; i < position; i++) {
            String lbandfc = "bandfc" + String.valueOf(i);
            int resID = getResources().getIdentifier(lbandfc, "id", mActivity.getPackageName());
            dummyEditText = (EditText) mActivity.findViewById(resID);
            dummyEditText.setTextColor(Color.parseColor("#000000"));
            dummyEditText.setFocusable(true);
            dummyEditText.setFocusableInTouchMode(true);

            String lbandgain = "bandgain" + String.valueOf(i);
            resID = getResources().getIdentifier(lbandgain, "id", mActivity.getPackageName());
            dummyEditText = (EditText) mActivity.findViewById(resID);
            dummyEditText.setTextColor(Color.parseColor("#000000"));
            dummyEditText.setFocusable(true);
            dummyEditText.setFocusableInTouchMode(true);

            String lbandQ = "bandQ" + String.valueOf(i);
            resID = getResources().getIdentifier(lbandQ, "id",mActivity.getPackageName());
            dummyEditText = (EditText) mActivity.findViewById(resID);
            dummyEditText.setTextColor(Color.parseColor("#000000"));
            dummyEditText.setFocusable(true);
            dummyEditText.setFocusableInTouchMode(true);
        }

        for (int i = position; i < EqCoeffDlg.Max_Stage; i++) {
            String lbandfc = "bandfc" + String.valueOf(i);
            int resID = getResources().getIdentifier(lbandfc, "id", mActivity.getPackageName());
            dummyEditText = (EditText) mActivity.findViewById(resID);
            dummyEditText.setFocusable(false);
            dummyEditText.setFocusableInTouchMode(false);
            dummyEditText.setTextColor(Color.parseColor("#ababab"));

            String lbandgain = "bandgain" + String.valueOf(i);
            resID = getResources().getIdentifier(lbandgain, "id", mActivity.getPackageName());
            dummyEditText = (EditText) mActivity.findViewById(resID);
            dummyEditText.setFocusable(false);
            dummyEditText.setFocusableInTouchMode(false);
            dummyEditText.setTextColor(Color.parseColor("#ababab"));

            String lbandQ = "bandQ" + String.valueOf(i);
            resID = getResources().getIdentifier(lbandQ, "id", mActivity.getPackageName());
            dummyEditText = (EditText) mActivity.findViewById(resID);
            dummyEditText.setFocusable(false);
            dummyEditText.setFocusableInTouchMode(false);
            dummyEditText.setTextColor(Color.parseColor("#ababab"));
        }
    }


    public boolean DoDataValidate()
    {
        EditText dummyEditText;
        double Max_Fc;
        double Min_Fc= 1;
        String inString = null;
        try
        {
            double mbandv;
            String fctext = "center frequency is out of range (Min : 1 , Max : 200000.0 )";
            String gaintext = "gain(db) is out of range (Min : -100 , Max : 20)";
            String qtext = "Q is out of range (Min : 0 , Max : 1000)";

            dummyEditText = (EditText) mActivity.findViewById(R.id.eqfs);
            inString = dummyEditText.getText().toString();
            mbandv = Double.valueOf(inString);
            if(mbandv<Min_Fc || mbandv > 200000.0) {
                dummyEditText.setError(fctext);
                //Toast.makeText(mActivity.getBaseContext(),gaintext,duration).show();
                return false;
            }
            mEqCoeffDlg.m_SampleFreq = mbandv;
            Max_Fc = mbandv/2;
            String fctext1 = "center frequency is out of range (Min : 1 , Max :" + Max_Fc + " )";

            for(int i=0;i<mEqCoeffDlg.m_StageNum;i++) {
                String lbandfc = "bandfc" + String.valueOf(i);
                int resID = getResources().getIdentifier(lbandfc, "id", mActivity.getPackageName());
                dummyEditText = (EditText) mActivity.findViewById(resID);
                inString = dummyEditText.getText().toString();
                mbandv = Double.valueOf(inString);

                if(mbandv<Min_Fc || mbandv >Max_Fc) {
                    dummyEditText.setError(fctext1);
                    return false;
                }
                mEqCoeffDlg.m_Freq[i] = mbandv;
                String lbandgain = "bandgain" + String.valueOf(i);
                resID = getResources().getIdentifier(lbandgain, "id", mActivity.getPackageName());
                dummyEditText = (EditText) mActivity.findViewById(resID);
                inString = dummyEditText.getText().toString();
                mbandv = Double.valueOf(inString);

                if(Double.isNaN(mbandv))
                {
                    dummyEditText.setError("Gain is Not a Number and set it properly");
                    return false;
                }
                if(mbandv<EqCoeffDlg.Min_gaindb || mbandv >EqCoeffDlg.Max_gaindb) {
                    dummyEditText.setError(gaintext);
                    return false;
                }
                mEqCoeffDlg.m_Gain[i] = mbandv;
                String lbandQ = "bandQ" + String.valueOf(i);
                resID = getResources().getIdentifier(lbandQ, "id", mActivity.getPackageName());
                dummyEditText = (EditText) mActivity.findViewById(resID);
                inString = dummyEditText.getText().toString();
                mbandv = Double.valueOf(inString);
                if(Double.isNaN(mbandv))
                {
                    dummyEditText.setError("Q is Not a Number and set it properly");
                    return false;
                }
                if(mbandv<EqCoeffDlg.Min_Q || mbandv >EqCoeffDlg.Max_Q) {
                    dummyEditText.setError(qtext);
                    return false;
                }
                mEqCoeffDlg.m_Q[i] = mbandv;
            }

            dummyEditText = (EditText) mActivity.findViewById(R.id.eqGlgain);
            inString = dummyEditText.getText().toString();
            mbandv = Double.valueOf(inString);
            if(Double.isNaN(mbandv))
            {
                dummyEditText.setError("Globalgain Not a Number and set it properly");
                return false;
            }
            if(mbandv<EqCoeffDlg.Min_gaindb || mbandv >EqCoeffDlg.Max_gaindb) {
                dummyEditText.setError(gaintext);
                return false;
            }
            mEqCoeffDlg.m_GlobalGain = mbandv;
            dummyEditText = (EditText) mActivity.findViewById(R.id.Eq_iteration_val);
            inString = dummyEditText.getText().toString();
            int mbandv1 = Integer.valueOf(inString);
            if(mbandv1<1 || mbandv1 >100) {
                String Itrtext = "Iteration is out of range (Min : 1 , Max : 100)";
                dummyEditText.setError(Itrtext);
                return false;
            }
            mEqCoeffDlg.m_IterVal = mbandv1;

            return true;
        }
        catch (NumberFormatException e)
        {
            if(inString.equals(""))
            {
                if(D) Log.d(TAG, "You've entered empty string.");
                Toast.makeText(mActivity.getBaseContext(),"You've entered empty string.",Toast.LENGTH_SHORT).show();
            }
            return false;
        }

    }

    public void EqLastStatePreserve()
    {
        SharedPreferences msharedPreferences;

        msharedPreferences = mActivity.getSharedPreferences(mActivity.getPackageName(), 0);
        Editor editor = msharedPreferences.edit();

        editor.putInt("EqGui_last_bandmode", Eqbandmode);
        editor.putInt("EqGui_last_stagemode", mEqCoeffDlg.m_StageNum);
        editor.putString("EqGui_last_sf", Double.toString(mEqCoeffDlg.m_SampleFreq));
        editor.putString("EqGui_last_gg", Double.toString(mEqCoeffDlg.m_GlobalGain));

        for(int i=0;i<mEqCoeffDlg.Max_Stage;i++) {
            editor.putString("EqGui_last_f"+i, Double.toString(mEqCoeffDlg.m_Freq[i]));
            editor.putString("EqGui_last_g"+i, Double.toString(mEqCoeffDlg.m_Gain[i]));
            editor.putString("EqGui_last_q"+i, Double.toString(mEqCoeffDlg.m_Q[i]));
        }

        editor.putInt("EqGui_last_iteration", mEqCoeffDlg.m_IterVal);
        editor.commit();
    }

    public void EqLastStateRestore()
    {
        String inString = null;
        SharedPreferences msp;

        msp = mActivity.getSharedPreferences(mActivity.getPackageName(), 0);

        mEqCoeffDlg.m_StageNum = msp.getInt("EqGui_last_stagemode", 0);

        inString = msp.getString("EqGui_last_sf", "48000.0");
        mEqCoeffDlg.m_SampleFreq = Double.valueOf(inString);

        inString = msp.getString("EqGui_last_gg","0.0");
        mEqCoeffDlg.m_GlobalGain = Double.valueOf(inString);

        for(int i=0;i<mEqCoeffDlg.Max_Stage;i++) {
            inString = msp.getString("EqGui_last_f" + i, "100");
            mEqCoeffDlg.m_Freq[i] = Double.valueOf(inString);

            inString = msp.getString("EqGui_last_g" + i, "0");
            mEqCoeffDlg.m_Gain[i] = Double.valueOf(inString);

            inString = msp.getString("EqGui_last_q" + i, "1.0");
            mEqCoeffDlg.m_Q[i] = Double.valueOf(inString);
        }
        mEqCoeffDlg.m_IterVal = msp.getInt("EqGui_last_iteration",20 );
    }

}
