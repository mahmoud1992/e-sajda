/*
 * Copyright (C) 2013 ISSC
 */
#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#include "test.h"
#include "typedef.h"
#include "basop.h"
#include "cst_lbc.h"
#include "tab_lbc.h"
#include "lbccodec.h"
#include "coder.h"
#include "decod.h"
#include "exc_lbc.h"
#include "util_lbc.h"
#include "cod_cng.h"
#include "dec_cng.h"
#include "vad.h"

#define printf(...) __android_log_print(ANDROID_LOG_DEBUG, "printfFromJNI", __VA_ARGS__)

/* Global variables */
enum  Wmode    WrkMode = Both ;
enum  Crate    WrkRate = Rate63 ;

int   PackedFrameSize[2] = {
   25 ,
   20
   } ;

Flag    UseHp = True ;
Flag    UsePf = True ;
Flag    UseVx = False ;
Flag    UsePr = False ;
long    chk_wav_file_format(FILE * test);
void    Line_Wr_Wav_format(Word16 *Line, FILE *Fp , int frame_no,char mode) ;
void    Line_Wr_Wav_format_up(Word16 *Line, FILE *out , int frame_no,char mode,int SR);
FILE * decode_in_fptr;
FILE * decode_out_fptr;
int SamplingRate;
int Channel_no;
char  SignOn[] = "ACL/USH/FT/DSPG ANSI C CODEC ITU LBC Ver 5.00\n" ;

jint
Java_com_issc_isscaudiowidget_CallerNameService_toVoicePrompt ( JNIEnv *env, jobject thiz)
{
	FILE    *Ifp, *Ofp ;            /* I/O File pointers */
	FILE    *Fep  = NULL;           /* Frame erasures file pointer */
	FILE    *Ratp = NULL;           /* Rate file pointer */
	FILE    * coef_out;
	FILE    *Cpt;
	long    FrCnt = 0 ;
	long    FlLen ;
	int i;
	Word16  DataBuff[Frame*48] ;
	Word16  Crc ;
	Word16  temp ;
	Word16  temp1 ;
	char Rate_Rd;
	int SR_t;
	int samplestepsize;
	char    Line[25] ;

	/* Process arguments and open I/O files */
	FlLen = Process_Files( &Ifp, &Ofp, &Fep, &Ratp) ;
	if (FlLen < 0) {
		return False;
	}

	/*
	  Init coder and the decoder
	 */
	Init_Coder( ) ;
	Init_Decod( ) ;

	/* Init Comfort Noise Functions */
	if( UseVx ) {
	    Init_Vad();
	    Init_Cod_Cng( );
	}
	Init_Dec_Cng( );

	///////////////////////////////////////////////////////////////////
	////////   Quantize the coefficient to 8bit resolutions    ////////
	///////////////////////////////////////////////////////////////////

#ifdef print_coef
	coef_out = fopen("AcbkGainTable085.txt","wb");
#endif

	for (i=0;i<NbFilt085*20;i++){
		temp = AcbkGainTable085[i];
		temp = ((Word16)abs(temp)>>8)<<8;
		if(AcbkGainTable085[i]<0){
			temp = -1*temp;
		}
		AcbkGainTable085[i] = temp;
	#ifdef print_coef
		if(i%2 == 0){
			temp1 = temp;
		}else{
		    temp1 = temp1 | ((temp>>8)&0x00FF);
		    fprintf(coef_out,"%d\n",temp1);
		}
	#endif
	}

#ifdef print_coef
	fclose(coef_out);
#endif

#ifdef print_coef
	coef_out = fopen("AcbkGainTable170.txt","wb");
#endif

	for (i=0;i<NbFilt170*20;i++) {
		temp = AcbkGainTable170[i];
		temp = ((Word16)abs(temp)>>8)<<8;
	    if(AcbkGainTable170[i]<0) {
	    	temp = -1*temp;
		}
		AcbkGainTable170[i] = temp;
	#ifdef print_coef
	    if(i%2 == 0) {
	    	temp1 = temp;
		} else{
		    temp1 = temp1 | ((temp>>8)&0x00FF);
			fprintf(coef_out,"%d\n",temp1);
		}
	#endif
	}

#ifdef print_coef
	fclose(coef_out);
#endif

#ifdef print_coef
	coef_out = fopen("Band0Tb8.txt","wb");
#endif
	for (i=0;i<LspCbSize*3;i++){
		temp = Band0Tb8[i];
		temp = ((Word16)abs(temp)>>8)<<8;
        if(Band0Tb8[i]<0){
        	temp = -1*temp;
        }
        Band0Tb8[i] = temp;
    #ifdef print_coef
	    if(i%2 == 0){
	    	temp1 = temp;
		} else {
			temp1 = temp1 | ((temp>>8)&0x00FF);
			fprintf(coef_out,"%d\n",temp1);
		}
    #endif
	}

#ifdef print_coef
	fclose(coef_out);
#endif

#ifdef print_coef
	coef_out = fopen("Band1Tb8.txt","wb");
#endif
	for (i=0;i<LspCbSize*3;i++){
		temp = Band1Tb8[i];
		temp = ((Word16)abs(temp)>>8)<<8;
		if(Band1Tb8[i]<0){
			temp = -1*temp;
	    }
	    Band1Tb8[i] = temp;
    #ifdef print_coef
	    if(i%2 == 0){
	    	temp1 = temp;
		} else {
			temp1 = temp1 | ((temp>>8)&0x00FF);
			fprintf(coef_out,"%d\n",temp1);
		}
    #endif
	}

#ifdef print_coef
	fclose(coef_out);
#endif

#ifdef print_coef
	coef_out = fopen("Band2Tb8.txt","wb");
#endif
	for (i=0;i<LspCbSize*4;i++){
		temp = Band2Tb8[i];
		temp = ((Word16)abs(temp)>>8)<<8;
		if(Band2Tb8[i]<0){
			temp = -1*temp;
		}
		Band2Tb8[i] = temp;
    #ifdef print_coef
	    if(i%2 == 0){
	    	temp1 = temp;
		} else {
		    temp1 = temp1 | ((temp>>8)&0x00FF);
		    fprintf(coef_out,"%d\n",temp1);
		}
    #endif
	}

#ifdef print_coef
	fclose(coef_out);
#endif

#ifdef print_coef
	coef_out = fopen("gain170.txt","wb");
#endif
	for (i=0;i<170;i++){
		temp = gain170[i];
		temp = ((Word16)abs(temp)>>8)<<8;
		if(gain170[i]<0){
			temp = -1*temp;
		}
		gain170[i] = temp;
    #ifdef print_coef
	    if(i%2 == 0){
	    	temp1 = temp;
		} else {
			temp1 = temp1 |((temp>>8)&0x00FF);
			fprintf(coef_out,"%d\n",temp1);
		}
    #endif
	}

#ifdef print_coef
	fclose(coef_out);
#endif

#ifdef print_coef
	coef_out = fopen("tabgain170.txt","wb");
#endif
	for (i=0;i<170;i++){
		temp = tabgain170[i];
		temp = ((Word16)abs(temp)>>8)<<8;
		if(tabgain170[i]<0){
			temp = -1*temp;
		}
		tabgain170[i] = temp;
	#ifdef print_coef
	    if(i%2 == 0){
	    	temp1 = temp;
		} else {
			temp1 = temp1 | (temp>>8);
			fprintf(coef_out,"%d\n",temp1);
		}
    #endif
	}

#ifdef print_coef
	fclose(coef_out);
#endif

#ifdef print_coef
	coef_out = fopen("tabgain85.txt","wb");
#endif
	for (i=0;i<85;i++){
	  temp = tabgain85[i];
	  temp = ((Word16)abs(temp)>>8)<<8;

	  if(tabgain85[i]<0){
	    temp = -1*temp;
	  }
	  tabgain85[i] = temp;
	#ifdef print_coef
	    if(i%2 == 0){
	    	temp1 = temp;
		} else {
			temp1 = temp1 | (temp>>8);
			fprintf(coef_out,"%d\n",temp1);
		}
    #endif
	}

#ifdef print_coef
	fclose(coef_out);
#endif
///////////////////////////////////////////////////////////////////////////
////////   Quantize the coefficient to 8bit resolutions   --  END ////////
//////////////////////////////////////////////////////////////////////////

	/* Process all the file to C format */
	switch ( WrkMode ) {
		case Both:
			chk_wav_file_format(Ifp);
	        Line_Wr_Wav_format( Line, Ofp, FlLen,0);
			Read_lbc( DataBuff, (Channel_no*(SamplingRate*Frame)/8000), Ifp,SamplingRate,1, Channel_no,&FlLen ) ;
	        break ;
	    case Cod :
	        if ( Ofp == NULL ) {
	            //exit(1) ;
	        	return False;
	        }
			chk_wav_file_format(Ifp);
	        Line_Wr_Coder_Cformat( Line, Ofp, FlLen,0) ;
			Read_lbc( DataBuff, (Channel_no*(SamplingRate*Frame)/8000), Ifp,SamplingRate,1 , Channel_no,&FlLen) ;
	        break ;
	    case Dec :
			SamplingRate = 8000;
			Channel_no = 1;
	        Line_Wr_Wav_format( Line, Ofp, FlLen,0);
	        break ;
	}

	/* Process all the input file */
    do {
        switch ( WrkMode ) {
            case Both:
                if(Ratp != NULL) {
                    fread((char *)&Rate_Rd, sizeof(char), 1, Ratp);
                    WrkRate = (enum Crate)Rate_Rd;
                }
                if ( WrkRate == Rate53) reset_max_time();
                Read_lbc( DataBuff, (Channel_no*(SamplingRate*Frame)/8000), Ifp,SamplingRate,0, Channel_no,&FlLen ) ;
                Coder( DataBuff, Line ) ;
				#ifdef print_assmeblydebug
				for(i=0;i<24;i=i+2){
                  fprintf(decode_in_fptr,"%04x\n",((Line[i]&0x00FF)) | (Line[i+1]&0x00FF)<<8);
				}
                #endif

                Decod( DataBuff, Line, (Word16) 0 ) ;
				#ifdef print_assmeblydebug
				for(i=0;i<240;i++)
                fprintf(decode_out_fptr,"%04x\n",DataBuff[i]&0xFFFF);
                #endif
				Line_Wr_Wav_format( DataBuff, Ofp, FlLen,1);
	            break ;

	        case Cod :
	            if(Ratp != NULL) {
	                fread((char *)&Rate_Rd, sizeof(char), 1, Ratp);
	                WrkRate = (enum Crate)Rate_Rd;
	            }
	            if ( WrkRate == Rate53) reset_max_time();
	            Read_lbc( DataBuff, (Channel_no*(SamplingRate*Frame)/8000), Ifp,SamplingRate,0, Channel_no,&FlLen ) ;
	            Coder( DataBuff, Line ) ;
	            Line_Wr_Coder_Cformat( Line, Ofp, FlLen,1) ;
				break ;

	        case Dec :
	            if(Line_Rd( Line, Ifp ) == (-1)) {
	                FlLen = FrCnt;
	                break;
	            }
	            Decod( DataBuff, Line, 0 ) ;
	            Line_Wr_Wav_format( DataBuff, Ofp, FlLen,1);
	            break ;
	    }

	    FrCnt ++ ;
	    if( UsePr) {
	        if( WrkMode == Dec) {
	            if(FrCnt < FlLen) {
	                fprintf( stdout, "Done : %6ld\r", FrCnt) ;
	            }
	        }
	        else {
	            fprintf( stdout, "Done : %6ld %3ld\r", FrCnt, FrCnt*100/FlLen ) ;
	        }
	        fflush(stdout);
	    }
	}   while ( FrCnt < FlLen ) ;

    /* Process all the file to C format */
    switch ( WrkMode ) {
    	case Both:
            break ;
    	case Cod :
            if ( Ofp == NULL ) {
            	return False;
                //exit(1) ;
            }
            Line_Wr_Coder_Cformat( Line, Ofp, FlLen,2) ;
            break ;
        case Dec :
            break ;
    }

    if(Ifp) { (void)fclose(Ifp); }
    if(Ofp) { (void)fclose(Ofp); }
    if(Fep) { (void)fclose(Fep); }
    if(Ratp) { (void)fclose(Ratp); }

	/******** test ********/
	/*FILE *fp = fopen("/sdcard/TTSTest/123.3gp","rb");
	if (fp == NULL) return False;
	else return True;*/

	return True;
}

/*
   This function processes the argument parameters. The function
      opens the IO files, and sets the global arguments accordingly
      to the command line parameters.
*/
long  Process_Files( FILE **Ifp, FILE **Ofp, FILE **Fep, FILE **Ratp)
{
    int     i ;
    long    Flen ;
    char    *FerFileName = NULL ;
    char    *RateFileName = NULL ;

    /* mode setting */
    WrkRate = Rate63 ;   // work rate setting
    WrkMode = Cod ;     // work mode setting

    *Ifp = fopen( "/sdcard/TTSTest/tts.wav", "rb") ;
    if ( *Ifp == NULL ) {
        fprintf(stderr, "Invalid input file name: tts.wav\n") ;
        return -1;
    }

    if ( UsePr )
        printf("Input  file:      tts.wav\n") ;

    *Ofp = fopen( "/sdcard/TTSTest/output.h", "wb") ;
    if ( *Ofp == NULL ) {
        fprintf(stderr, "Can't open output file: output.wav\n") ;
        return -2;
    }

    if ( UsePr )
        printf("Output file:     output.h\n") ;

    /* Open Fer file if required */
    if ( WrkMode == Dec ) {

    }
    else {
        if ( RateFileName != NULL ) {
            *Ratp = fopen( RateFileName, "rb" ) ;
            if ( *Ratp == NULL ) {
                fprintf(stderr, "Can't open Rate file: %s\n", RateFileName ) ;
                //exit(1) ;
                return -3;
            }
            if ( UsePr )
                printf("Rate   file:     %s\n", RateFileName ) ;
        }
    }

    /* Options report */
    if ( UsePr ) {

        printf("Options:\n");
        if (WrkMode == Both )
            printf("Encoder/Decoder\n");
        else if (WrkMode == Cod )
            printf("Encoder\n");
        else
            printf("Decoder\n");


        if( WrkMode != Cod ) {
            if (UsePf == 0 )
                printf("Postfilter disabled\n");
            else
                printf("Postfilter enabled\n");
        }

        if( WrkMode <= Cod ) {
            if(*Ratp == NULL) {
                if (WrkRate == Rate63 )
                    printf("Rate 6.3 kb/s\n");
                else
                    printf("Rate 5.3 kb/s\n");
            }
            if (UseHp == 0 )
                printf("Highpassfilter disabled\n");
            else
                printf("Highpassfilter enabled\n");
            if (UseVx == 0 )
                printf("VAD/CNG disabled\n");
            else
                printf("VAD/CNG enabled\n");
        }
    }

    /*
      Compute the file length
    */
    fseek( *Ifp, 0L, SEEK_END ) ;
    Flen = ftell( *Ifp ) ;
    rewind( *Ifp ) ;
    if ( WrkMode == Dec )
        //Flen = 0x7fffffffL ;
		Flen /= 24;
    else
        Flen /= sizeof(Word16)*Frame ;

    return Flen ;
}

long     chk_wav_file_format(FILE * in)
{
	char     tmp1[5], *pp;
	short    ii2;
	long int ii4;
	long int length;

    fread(&tmp1[0],sizeof(char),4,in);
    tmp1[4]=0;      pp=&tmp1[0];
    if( strcmp(pp,"RIFF") !=0 )
    {
    	rewind(in);
        return 0;
    }
    else
    {
    	fread(&length, sizeof(long int),1,in);     //length
        fread(&tmp1[0],sizeof(char),4,in);      //WAVE
        fread(&tmp1[0],sizeof(char),4,in);      //fmt
        fread(&ii4, sizeof(long int),1,in);     //length
        fread(&ii2, sizeof(short),1,in);          //formattag
        if(ii2!=1)
        {
            system("CLS");
            if ( UsePr ) printf("Invalid wav file format: Not PCM Format\n");
            return -1;
        }
        fread(&ii2, sizeof(short),1,in);          //Channel
		Channel_no = ii2;
        if(ii2!=1)
        {
          	if ( UsePr ) printf("Invalid wav file format: Not MONO mode\n");
        }
        fread(&ii4, sizeof(long int),1,in);     //sampling rate
        SamplingRate = ii4;
		if(ii4!=0x1f40)
        {
			//if ( UsePr ) printf("Invalid wav file format: Not 8K Sampling rate\n");
        }
        fread(&ii4, sizeof(long int),1,in);     //Bytes/Sec
        if(ii4!=0x3e80)
        {
           	//if ( UsePr ) printf("Invalid wav file format: Not 16 bit format\n");
        }
        fread(&ii2, sizeof(short),1,in);        //Bytes/Sample
        fread(&ii2, sizeof(short),1,in);        //Bits/Sample
        fread(&tmp1[0],sizeof(char),4,in);      //"data"
        fread(&ii4, sizeof(long int),1,in);     //length

        }
	return ((length-36)>>1);
}

void      Line_Wr_Wav_format(Word16 *Line, FILE *out , int frame_no,char mode) {
	long ByteRate,Chunk2Size,Chunk1Size,Freq,BitNumLong;
	short BitNum,BlockAlign,unit1;
	int i ;
	if(mode ==0) {
	  	ByteRate   = 8000*1*16/8;
	    BlockAlign = 1*16/8;
	    Chunk2Size = (((long)(frame_no*2*240*8000))/((long)SamplingRate*Channel_no));
        Chunk1Size=Chunk2Size+36;
        BitNum=16;
        BitNumLong=16;
        Freq=8000;
        unit1=1;
	    fwrite("RIFF",1,4,out);
	    fwrite(&Chunk1Size,4,1,out);
	    fwrite("WAVEfmt ",1,8,out);

		fwrite(&BitNumLong,4,1,out);
		fwrite(&unit1,2,1,out);
		fwrite(&unit1,2,1,out);
		fwrite(&Freq,4,1,out);
		fwrite(&ByteRate,4,1,out);
		fwrite(&BlockAlign,2,1,out);
		fwrite(&BitNum,2,1,out);

		fwrite("data",1,4,out);
		fwrite(&Chunk2Size,4,1,out);
	}
	if(mode == 1) {
		for(i=0;i<Frame;i++){
		  fprintf(out,"%c",Line[i]&0x00FF);
		  fprintf(out,"%c",(Line[i]>>8)&0x00FF);
		}
	}
}
