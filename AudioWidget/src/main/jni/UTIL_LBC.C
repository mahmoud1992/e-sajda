/*
**
** File:    util_lbc.c
**
** Description: utility functions for the lbc codec
**
** Functions:
**
**  I/O functions:
**
**      Read_lbc()
**      Write_lbc()
**
**  High-pass filtering:
**
**      Rem_Dc()
**
**  Miscellaneous signal processing functions:
**
**      Vec_Norm()
**      Mem_Shift()
**      Comp_En()
**      Scale()
**
**  Bit stream packing/unpacking:
**
**      Line_Pack()
**      Line_Unpk()
**
**  Mathematical functions:
**
**      Sqrt_lbc()
**      Rand_lbc()
*/

/*
    ITU-T G.723 Speech Coder   ANSI-C Source Code     Version 5.00
    copyright (c) 1995, AudioCodes, DSP Group, France Telecom,
    Universite de Sherbrooke.  All rights reserved.
*/


#include <stdlib.h>
#include <stdio.h>
#include <math.h>

#include "typedef.h"
#include "basop.h"
#include "cst_lbc.h"
#include "tab_lbc.h"
#include "lbccodec.h"
#include "coder.h"
#include "decod.h"
#include "util_lbc.h"
#include "Coef_Fs48K_cutoff4k.h"
/*
**
** Function:        Read_lbc()
**
** Description:     Read in a file
**
** Links to text:   Sections 2.2 & 4
**
** Arguments:
**
**  Word16 *Dpnt
**  int     Len
**  FILE *Fp
**
** Outputs:
**
**  Word16 *Dpnt
**
** Return value:    None
**
*/
int round_float(double number)
{
    return (number >= 0) ? (int)(number + 0.5) : (int)(number - 0.5);
}

void  Read_lbc( Word16 *Dpnt, int Len, FILE *Fp, int SampleRate,char reset,int Channel_no, long int *FlLen )
{
    int   i  ;
	int k;
	int Acc0;
    static int FIR_Temp[58];
	int TempOut[240*48];
	int TempIn[240*48];
	static int Coef[58];
    static float sample_stepsize;
if(reset)
{
	  memset(&FIR_Temp, 0, sizeof(FIR_Temp));
	  switch(SampleRate){
		case 48000:
			for(i=0;i<58;i++){
			  Coef[i] = (int)(32767*Coef_Fs48K_cutoff4k[i]*0.9);
			}
			sample_stepsize = 6;
			*FlLen = *FlLen/6;
			break;
		case 44100:
			for(i=0;i<58;i++){
				Coef[i] = (int)(32767*Coef_Fs44p1K_cutoff4k[i]);
			}
			sample_stepsize = 5.52;
			*FlLen = *FlLen/5.52;
			break;	  
		case 32000:
			for(i=0;i<58;i++){
			  Coef[i] = (int)(32767*Coef_Fs32K_cutoff4k[i]);
			}
			sample_stepsize = 4;
			*FlLen = *FlLen/4;
			break;	  
		case 22050:
			for(i=0;i<58;i++){
				Coef[i] = (int)(32767*Coef_Fs22p05K_cutoff4k[i]);
			}
			sample_stepsize = 5.52/2;
			*FlLen = *FlLen/(5.52/2);
			break;
		case 16000:
			for(i=0;i<58;i++){
			  Coef[i] = (int)(32767*Coef_Fs16K_cutoff4k[i]);
			}
			sample_stepsize = 2;
			*FlLen = *FlLen/2;
			break;
		default:
			Coef[0]=32767;
			sample_stepsize = 1;
			for(i=1;i<58;i++){
			  Coef[i] = 0;
			}
	  }


}else{
    for ( i = 0 ; i < Len ; i ++ )
        Dpnt[i] = (Word16) 0 ;
    
    fread ( (char *)Dpnt, sizeof(Word16), Len, Fp ) ;
	if(Channel_no >=2){
	  for(k=0;k<Len/2;k++)
	  {
	    TempIn[k] = (Dpnt[2*k]>>1)+(Dpnt[2*k+1]>>1);

	  }
	  Len = Len/Channel_no;
	}else{
	  for(k=0;k<Len;k++)
	  {
	    TempIn[k] = Dpnt[k];

	  }	   
	}

	/* 20130823 modified */

	  for(k=0;k<Len;k++)
	  {
	    TempIn[k] = (TempIn[k]>>2)  + (TempIn[k] >>1);
	  }

	if(SampleRate!=8000){
  	  for(k=0;k<Len;k++){
		  Acc0=0;
		  for(i=0;i<58;i++){
	         Acc0 = L_mac(Acc0,FIR_Temp[i],Coef[i]);
		  }
          /* Update FIR memory */
          for ( i = 58-1 ; i > 0 ; i -- )
            FIR_Temp[i] = FIR_Temp[i-1] ;
         // FIR_Temp[0] = Dpnt[k] ;
		   FIR_Temp[0] = TempIn[k] ;
          TempOut[k] = round_2( Acc0 );
	  }
	  //downsample
	  for(k=0;k<Frame;k++){
	    //Dpnt[k] = TempOut[(int)(((float)k)*sample_stepsize)];
		 Dpnt[k] = TempOut[round_float(k*sample_stepsize)]; 
	  }
	}
}







    return;
}


int  Read_lbc_up( Word16 *Dpnt, int Len, FILE *Fp, int SampleRate,char reset,int Channel_no )
{
    int   i  ;
	int k;
	int Acc0;
    static int FIR_Temp[58];
    static int FIR_Temp_1[58];	
	int TempIn[32];
	int TempIn_1[32];
	static int TempIn_2[32];
	static int FIR_Temp_JB[4];
	static int Coef[64];
	static int Coef_1[64];
	static int Coef_JB[4];
    static int sample_stepsize;
	static int frac_step;
	int frac_cnt;
	int frac_cnt_temp;
	int frac_cnt_temp_JB;
	int   InputSamp_cnt;
    static int updateJBFIR=1;
if(reset)
{
	updateJBFIR = 1;
	  memset(&FIR_Temp, 0, sizeof(FIR_Temp));
	  memset(&FIR_Temp_1, 0, sizeof(FIR_Temp_1));
	  memset(&TempIn_2, 0, sizeof(TempIn_2));
	  memset(&FIR_Temp_JB, 0, sizeof(FIR_Temp_JB));
		//for(i=0;i<13;i++){
		//  Coef_1[i] = (int)(32767*G7231_FIR[i]);
		//}
		for(i=0;i<58;i++){
		  Coef[i] = (int)(32767*FIR_Coff_4k_SR_16k[i]);
		}
	  switch(SampleRate){
		case 48000:
			for(i=0;i<16;i++){
			  Coef_1[i] = (int)(32767*FIR_Coff_4k_SR_24k[i]);
			}
			sample_stepsize = 11;
			frac_step = (int)(16383*11/32);
			
			break;
		case 44100:
			for(i=0;i<16;i++){
				Coef_1[i] = (int)(32767*FIR_Coff_4k_SR_22k[i]);
			}
			sample_stepsize = 12;
			frac_step =  (int)(16383*12/32);
			break;	  
		case 32000:
			for(i=0;i<16;i++){
			  Coef_1[i] = (int)(32767*FIR_Coff_4k_SR_16k[i]);
			}
			sample_stepsize = 16;
			frac_step =  (int)(16383*16/32);
			break;
		case 22050:
			for(i=0;i<16;i++){
				Coef[i] = (int)(32767*Coef_Fs22p05K_cutoff4k[i]);
			}
			sample_stepsize = 12;
			frac_step =  (int)(16383*12/32);
			break;
		case 16000:
//			for(i=0;i<58;i++){
//			  Coef[i] = (int)(32767*Coef_Fs16K_cutoff4k[i]);
//			}
			sample_stepsize = 32;
			frac_step = (int)(16383*32/32);
			break;
		default:
			Coef[0]=32767;
			sample_stepsize = 1;
	//		for(i=1;i<58;i++){
	//		  Coef[i] = 0;
	//		}
	  }


}else{
    for ( i = 0 ; i < sample_stepsize ; i ++ )   Dpnt[i] = (Word16) 0 ;
    
    fread ( (char *)Dpnt, sizeof(Word16), sample_stepsize, Fp ) ;
    


     TempIn_2[0] = TempIn_2[sample_stepsize];
	for(k=0;k<sample_stepsize;k++)
    {
	    TempIn_2[k+1] = Dpnt[k];
    }	   
 
		  if(SampleRate != 16000){
		  
		  



	//switch(SampleRate){
	//	case 48000:
			frac_cnt = 0;
			frac_cnt_temp = 16384;
			InputSamp_cnt = 0;
          // linear interpolation from 11 point to 32 point
		  for(i=0;i<32;i++){
			if(frac_cnt_temp>16383) {
			  frac_cnt_temp-=16384;
              
			  updateJBFIR = 1;
              for(k=3;k>0;k--){
                FIR_Temp_JB[k] = FIR_Temp_JB[k-1];
			  }
			  FIR_Temp_JB[0] = TempIn_2[InputSamp_cnt];
			  InputSamp_cnt ++;
			}
			else{
			  updateJBFIR = 0;
			}
            frac_cnt = frac_cnt_temp<<1;

		    //Acc0 = L_mult(TempIn_2[InputSamp_cnt],32767- frac_cnt);
			//Acc0=  L_mac( Acc0, TempIn_2[InputSamp_cnt+1], (frac_cnt) ) ;
			//TempIn_1[i] = round(Acc0);
			  
			  frac_cnt_temp_JB = frac_cnt_temp/2;
			  Coef_JB[0] = mult(frac_cnt_temp_JB,frac_cnt_temp_JB);
			  Coef_JB[0] -= frac_cnt_temp_JB;
			   Coef_JB[0] =  Coef_JB[0]>>1;
              Coef_JB[1] = -1*mult(frac_cnt_temp_JB,frac_cnt_temp_JB);
			  Coef_JB[1] += 8192;
			  Coef_JB[2] = mult(frac_cnt_temp_JB,frac_cnt_temp_JB);
			  Coef_JB[2] += frac_cnt_temp_JB;
			  Coef_JB[2] =  Coef_JB[2]>>1;
			  
              /*
              frac_cnt_temp_JB = frac_cnt_temp/2;
			  Coef_JB[0] = mult(frac_cnt_temp_JB,mult(frac_cnt_temp_JB,frac_cnt_temp_JB));
			  Coef_JB[0] -= 3*mult(frac_cnt_temp_JB,frac_cnt_temp_JB);
			  Coef_JB[0] +=  2*frac_cnt_temp_JB;
              Coef_JB[0] /= -6;
              Coef_JB[1] =  mult(frac_cnt_temp_JB,mult(frac_cnt_temp_JB,frac_cnt_temp_JB));
			  Coef_JB[1] -= 2*mult(frac_cnt_temp_JB,frac_cnt_temp_JB);
              Coef_JB[1] -=  frac_cnt_temp_JB;
			  Coef_JB[1] +=  16384;
              Coef_JB[1] /=2;
			  Coef_JB[2] =mult(frac_cnt_temp_JB,mult(frac_cnt_temp_JB,frac_cnt_temp_JB));
			  Coef_JB[2] -= 1*mult(frac_cnt_temp_JB,frac_cnt_temp_JB);
			  Coef_JB[2] +=  2*frac_cnt_temp_JB;
              Coef_JB[2] /= -2;
			  Coef_JB[3] =mult(frac_cnt_temp_JB,mult(frac_cnt_temp_JB,frac_cnt_temp_JB));
			  Coef_JB[3] -= 1*frac_cnt_temp_JB;
			  Coef_JB[3] /= 6;
              */
			  Acc0 = 0;
            for(k=0;k<3;k++){
               Acc0 = L_mac(Acc0,FIR_Temp_JB[2-k],Coef_JB[k]);
			}

		    if(updateJBFIR ==1){
            //  for(k=3;k>0;k--){
            //    FIR_Temp_JB[k] = FIR_Temp_JB[k-1];
			//  }
			//  FIR_Temp_JB[0] = TempIn_2[InputSamp_cnt];
		    }

			TempIn_1[i] = round_2(Acc0*4);
			
            frac_cnt_temp+=frac_step;
		  }

///////////////////////////////////////////////////////
//    Low Pass Filtering  32k SR cutoff 4k
///////////////////////////////////////////////////////
		  for(k=0;k<32;k++){
		    Acc0=0;
		    for(i=0;i<16;i++){ Acc0 = L_mac(Acc0,FIR_Temp_1[i],Coef_1[i]);  }
          /* Update FIR memory */
          for ( i = 16-1 ; i > 0 ; i -- )
            FIR_Temp_1[i] = FIR_Temp_1[i-1] ;
         // FIR_Temp[0] = Dpnt[k] ;
		    FIR_Temp_1[0] = TempIn_1[k] ;
            TempIn[k] = round_2( Acc0 );
		  }
///////////////////////////////////////////////////////
//    Interpolation from 32points to 64points
///////////////////////////////////////////////////////
		  }else{
	for(k=0;k<sample_stepsize;k++)
    {
	    TempIn[k] = Dpnt[k];
    }			  
		  
		  }
		  for(k=0;k<64;k++){
			  Acc0=0;
               
			  for(i=(k%2);i<16;i+=2){
                Acc0 = L_mac(Acc0,FIR_Temp[i],Coef[i]);
			  }
            for ( i = 16-1 ; i > 0 ; i -- )
              FIR_Temp[i] = FIR_Temp[i-1] ;		  
		    if(k%2 == 0) FIR_Temp[0] = 0; else FIR_Temp[0] = TempIn[k>>1];
            Dpnt[k] = round_2( Acc0*2 );
		  }
	//	break;
	//	default:
	//	break;
	//}
/*
  	  for(k=0;k<sample_stepsize;k++){
		  Acc0=0;
		  for(i=0;i<58;i++){
	         Acc0 = L_mac(Acc0,FIR_Temp[i],Coef[i]);
		  }
          for ( i = 58-1 ; i > 0 ; i -- )
            FIR_Temp[i] = FIR_Temp[i-1] ;
         // FIR_Temp[0] = Dpnt[k] ;
		   FIR_Temp[0] = TempIn[k] ;
          TempOut[k] = round( Acc0 );
	  }
	  //downsample
	  for(k=0;k<Frame;k++){
	    //Dpnt[k] = TempOut[(int)(((float)k)*sample_stepsize)];
		 Dpnt[k] = TempOut[round_float(k*sample_stepsize)]; 
	  }
*/

}







    return sample_stepsize;
}



/*
**
** Function:        Write_lbc()
**
** Description:     Write a file
**
** Links to text:   Section
**
** Arguments:
**
**  Word16 *Dpnt
**  int     Len
**  FILE *Fp
**
** Outputs:         None
**
** Return value:    None
**
*/
void    Write_lbc( Word16 *Dpnt, int Len, FILE *Fp )
{
    fwrite( (char *)Dpnt, sizeof(Word16), Len, Fp ) ;
}

void Line_Wr_Coder_Cformat( char *Line, FILE *Fp , int frame_no,int mode)
{
  	int bytelen;
    static int lineword_cnt = 0;
	static int frame_cnt = 0;
	int i;
	bytelen = frame_no*24;
	if(mode == 0){
     	/*fprintf(Fp,"unsigned char code BT_DSP_Code_PM1[] = \n{ \n");
        fprintf(Fp,"0x%02x, ",(bytelen)>>8 );
		fprintf(Fp,"0x%02x, ",(bytelen)&0x00FF );*/

		//fprintf(Fp,"%02x",(bytelen)>>8 );
		//fprintf(Fp,"%02x",(bytelen)&0x00FF );

		//lineword_cnt += 2;
	}
    if(mode == 1){
		for(i=0; i<12;i++){
			/*fprintf(Fp,"0x%02x, ",Line[2*i+1]&0x00FF );
			fprintf(Fp,"0x%02x, ",Line[2*i]&0x00FF );
		  
			lineword_cnt +=2;
			if(lineword_cnt%16 == 0 ){
				lineword_cnt =0;
				fprintf(Fp,"\n");
			}*/

			fprintf(Fp,"%02x",Line[2*i+1]&0x00FF );
			fprintf(Fp,"%02x",Line[2*i]&0x00FF );

			lineword_cnt +=2;
			if(lineword_cnt%480 == 0 ){
				lineword_cnt =0;
				fprintf(Fp,"\n");
			}
		}
	}
	if(mode == 2){
		fprintf(Fp,"\n");
		/*fprintf(Fp,"\n};\n");
		fprintf(Fp,"#define BT_DSP_CODE_PM1_TERMINATION 0x%08x\n",0);
		fprintf(Fp,"#define BT_DSP_CODE_PM1_START       0x%08x\n",0);
		fprintf(Fp,"#define BT_DSP_CODE_PM1_FINISH      0x%08x\n",bytelen+2);
		fprintf(Fp,"#define BT_DSP_CODE_PM1_LENGTH      0x%08x\n",bytelen+2);*/
	}
}

void    Line_Wr( char *Line, FILE *Fp )
{
    Word16  Info ;
    int     Size   ;

    Info = Line[0] & (Word16)0x0003 ;

    /* Check frame type and rate informations */
    switch (Info) {

    //    case 0x0002 : {   /* SID frame */
    //        Size  = 4;
    //        break;
    //    }

     //   case 0x0003 : {  /* untransmitted silence frame */
     //       Size  = 1;
     //       break;
     //   }

    //    case 0x0001 : {   /* active frame, low rate */
    //        Size  = 20;
    //        break;
    //    }

        default : {  /* active frame, high rate */
            Size  = 24;
        }
    }
    fwrite( Line, Size , 1, Fp ) ;
}

int Line_Rd( char *Line, FILE *Fp )
{
    Word16  Info ;
    int     Size   ;

    if(fread( Line, 1,1, Fp ) != 1) return(-1);

    //Info = Line[0] & (Word16)0x0003 ;
    Info = 0;
    /* Check frame type and rate informations */
    switch(Info) {

        /* Active frame, high rate */
        case 0 : {
            Size  = 23;
            break;
        }

        /* Active frame, low rate */
        case 1 : {
            Size  = 19;
            break;
        }

        /* Sid Frame */
        case 2 : {
            Size  = 3;
            break;
        }

        /* untransmitted */
        default : {
            return(0);
        }
    }
    fread( &Line[1], Size , 1, Fp ) ;
    return(0);
}

/*
**
** Function:        Rem_Dc()
**
** Description:     High-pass filtering
**
** Links to text:   Section 2.3
**
** Arguments:
**
**  Word16 *Dpnt
**
** Inputs:
**
**  CodStat.HpfZdl  FIR filter memory from previous frame (1 word)
**  CodStat.HpfPdl  IIR filter memory from previous frame (1 word)
**
** Outputs:
**
**  Word16 *Dpnt
**
** Return value:    None
**
*/
void  Rem_Dc( Word16 *Dpnt )
{
    int   i  ;


    Word32   Acc0,Acc1 ;

    if ( UseHp ) {
        for ( i = 0 ; i < Frame ; i ++ ) {

            /* Do the Fir and scale by 2 */
            Acc0 = L_mult( Dpnt[i], (Word16) 0x4000 ) ;
            Acc0 = L_mac ( Acc0, CodStat.HpfZdl, (Word16) 0xc000 ) ;
            CodStat.HpfZdl = Dpnt[i] ;

            /* Do the Iir part */
            Acc1 = L_mls( CodStat.HpfPdl, (Word16) 0x7f00 ) ;
            Acc0 = L_add( Acc0, Acc1 ) ;
            CodStat.HpfPdl = Acc0 ;
            Dpnt[i] = round_2(Acc0) ;
        }
    }
    else {
        for ( i = 0 ; i < Frame ; i ++ )
            Dpnt[i] = shr( Dpnt[i], (Word16) 1 ) ;
    }

    return;
}

/*
**
** Function:        Vec_Norm()
**
** Description:     Vector normalization
**
** Links to text:
**
** Arguments:
**
**  Word16 *Vect
**  Word16 Len
**
** Outputs:
**
**  Word16 *Vect
**
** Return value:  The power of 2 by which the data vector multiplyed.
**
*/
Word16  Vec_Norm( Word16 *Vect, Word16 Len )
{
    int   i  ;

    Word16  Acc0,Acc1   ;
    Word16  Exp   ;
    Word16  Rez ;
    Word32  Temp  ;

    static   short ShiftTable[16] = {
      0x0001 ,
      0x0002 ,
      0x0004 ,
      0x0008 ,
      0x0010 ,
      0x0020 ,
      0x0040 ,
      0x0080 ,
      0x0100 ,
      0x0200 ,
      0x0400 ,
      0x0800 ,
      0x1000 ,
      0x2000 ,
      0x4000 ,
      0x7fff
   } ;

    /* Find absolute maximum */
    Acc1 = (Word16) 0 ;
    for ( i = 0 ; i < Len ; i ++ ) {
        Acc0 = abs_s( Vect[i] ) ;
        if ( Acc0 > Acc1 )
            Acc1 = Acc0 ;
    }

    /* Get the shift count */
    Rez = norm_s( Acc1 ) ;
    Exp = ShiftTable[Rez] ;

    /* Normalize all the vector */
    for ( i = 0 ; i < Len ; i ++ ) {
        Temp = L_mult( Exp, Vect[i] ) ;
        Temp = L_shr( Temp, 4 ) ;
        Vect[i] = extract_l( Temp ) ;
    }

    Rez = sub( Rez, (Word16) 3) ;
    return Rez ;
}

/*
**
** Function:        Mem_Shift()
**
** Description:     Memory shift, update of the high-passed input speech signal
**
** Links to text:
**
** Arguments:
**
**  Word16 *PrevDat
**  Word16 *DataBuff
**
** Outputs:
**
**  Word16 *PrevDat
**  Word16 *DataBuff
**
** Return value:    None
**
*/
void  Mem_Shift( Word16 *PrevDat, Word16 *DataBuff )
{
    int   i  ;

    Word16   Dpnt[Frame+LpcFrame-SubFrLen] ;

    /*  Form Buffer  */
    for ( i = 0 ; i < LpcFrame-SubFrLen ; i ++ )
        Dpnt[i] = PrevDat[i] ;
    for ( i = 0 ; i < Frame ; i ++ )
        Dpnt[i+LpcFrame-SubFrLen] = DataBuff[i] ;

    /* Update PrevDat */
    for ( i = 0 ; i < LpcFrame-SubFrLen ; i ++ )
        PrevDat[i] = Dpnt[Frame+i] ;

    /* Update DataBuff */
    for ( i = 0 ; i < Frame ; i ++ )
        DataBuff[i] = Dpnt[(LpcFrame-SubFrLen)/2+i] ;

    return;
}

/*
**
** Function:        Line_Pack()
**
** Description:     Packing coded parameters in bitstream of 16-bit words
**
** Links to text:   Section 4
**
** Arguments:
**
**  LINEDEF *Line     Coded parameters for a frame
**  char    *Vout     bitstream chars
**  Word16   VadBit   Voice Activity Indicator
**
** Outputs:
**
**  Word16 *Vout
**
** Return value:    None
**
*/
void    Line_Pack( LINEDEF *Line, char *Vout, Word16 Ftyp )
{
    int     i ;
    int     BitCount ;

    Word16  BitStream[200] ;
    Word16 *Bsp = BitStream ;
    Word32  Temp ;

    /* Clear the output vector */
    for ( i = 0 ; i < 25 ; i ++ )
        Vout[i] = 0 ;

 /*
  * Add the coder rate info and frame type info to the 2 msb
  * of the first word of the frame.
  * The signaling is as follows:
  *     Ftyp  WrkRate => X1X0
  *       1     Rate63     00  :   High Rate
  *       1     Rate53     01  :   Low  Rate
  *       2       x        10  :   Silence Insertion Descriptor frame
  *       0       x        11  :   Used only for simulation of
  *                                 untransmitted silence frames
  */
    switch (Ftyp) {

        case 0 : {
            Temp = 0x00000003L;
            break;
        }

        case 2 : {
            Temp = 0x00000002L;
            break;
        }

        default : {
            if ( WrkRate == Rate63 )
                Temp = 0x00000000L ;
            else
                Temp = 0x00000001L ;
            break;
        }
    }

    /* Serialize Control info */
   // Bsp = Par2Ser( Temp, Bsp, 2 ) ;


    /* Check for Speech/NonSpeech case */
    if ( Ftyp == 1 ) {

    /* 24 bit LspId */
    Temp = (*Line).LspId ;
    Bsp = Par2Ser( Temp, Bsp, 24 ) ;

 /*
  * Do the part common to both rates
  */

        /* Adaptive code book lags */
        Temp = (Word32) (*Line).Olp[0] - (Word32) PitchMin ;
        Bsp = Par2Ser( Temp, Bsp, 7 ) ;

        Temp = (Word32) (*Line).Sfs[1].AcLg ;
        Bsp = Par2Ser( Temp, Bsp, 2 ) ;

        Temp = (Word32) (*Line).Olp[1] - (Word32) PitchMin ;
        Bsp = Par2Ser( Temp, Bsp, 7 ) ;

        Temp = (Word32) (*Line).Sfs[3].AcLg ;
        Bsp = Par2Ser( Temp, Bsp, 2 ) ;

        /* Write combined 12 bit index of all the gains */
        for ( i = 0 ; i < SubFrames ; i ++ ) {
            Temp = (*Line).Sfs[i].AcGn*NumOfGainLev + (*Line).Sfs[i].Mamp ;
            if ( WrkRate == Rate63 )
                Temp += (Word32) (*Line).Sfs[i].Tran << 11 ;
            Bsp = Par2Ser( Temp, Bsp, 12 ) ;
        }

        /* Write all the Grid indices */
        for ( i = 0 ; i < SubFrames ; i ++ )
            *Bsp ++ = (*Line).Sfs[i].Grid ;

        /* High rate only part */
        if ( WrkRate == Rate63 ) {

            /* Write the reserved bit as 0 */
           // *Bsp ++ = (Word16) 0 ;

            /* Write 13 bit combined position index */
          //  Temp = (*Line).Sfs[0].Ppos >> 16 ;
          //  Temp = Temp * 9 + ( (*Line).Sfs[1].Ppos >> 14) ;
          // Temp *= 90 ;
          //  Temp += ((*Line).Sfs[2].Ppos >> 16) * 9 + ( (*Line).Sfs[3].Ppos >> 14 ) ;
          //  Bsp = Par2Ser( Temp, Bsp, 13 ) ;
            Temp = (*Line).Sfs[0].Ppos >> 16 ;
            Temp = Temp * 16 + ( (*Line).Sfs[1].Ppos >> 14) ;
            Temp *= 256 ;
            Temp += ((*Line).Sfs[2].Ppos >> 16) * 16 + ( (*Line).Sfs[3].Ppos >> 14 ) ;
            Bsp = Par2Ser( Temp, Bsp, 16 ) ;

            /* Write all the pulse positions */
            Temp = (*Line).Sfs[0].Ppos & 0x0000ffffL ;
            Bsp = Par2Ser( Temp, Bsp, 16 ) ;

            Temp = (*Line).Sfs[1].Ppos & 0x00003fffL ;
            Bsp = Par2Ser( Temp, Bsp, 14 ) ;

            Temp = (*Line).Sfs[2].Ppos & 0x0000ffffL ;
            Bsp = Par2Ser( Temp, Bsp, 16 ) ;

            Temp = (*Line).Sfs[3].Ppos & 0x00003fffL ;
            Bsp = Par2Ser( Temp, Bsp, 14 ) ;

            /* Write pulse amplitudes */
            Temp = (Word32) (*Line).Sfs[0].Pamp ;
            Bsp = Par2Ser( Temp, Bsp, 6 ) ;

            Temp = (Word32) (*Line).Sfs[1].Pamp ;
            Bsp = Par2Ser( Temp, Bsp, 5 ) ;

            Temp = (Word32) (*Line).Sfs[2].Pamp ;
            Bsp = Par2Ser( Temp, Bsp, 6 ) ;

            Temp = (Word32) (*Line).Sfs[3].Pamp ;
            Bsp = Par2Ser( Temp, Bsp, 5 ) ;
        }

        /* Low rate only part */
        else {

            /* Write 12 bits of positions */
            for ( i = 0 ; i < SubFrames ; i ++ ) {
                Temp = (*Line).Sfs[i].Ppos ;
                Bsp = Par2Ser( Temp, Bsp, 12 ) ;
            }

            /* Write 4 bit Pamps */
            for ( i = 0 ; i < SubFrames ; i ++ ) {
                Temp = (*Line).Sfs[i].Pamp ;
                Bsp = Par2Ser( Temp, Bsp, 4 ) ;
            }
        }

    }

    else if(Ftyp == 2) {   /* SID frame */

        /* 24 bit LspId */
        Temp = (*Line).LspId ;
        Bsp = Par2Ser( Temp, Bsp, 24 ) ;

        /* Do Sid frame gain */
        Temp = (Word32)(*Line).Sfs[0].Mamp ;
        Bsp = Par2Ser( Temp, Bsp, 6 ) ;
    }

    /* Write out active frames */
    if ( Ftyp == 1 ) {
        if ( WrkRate == Rate63 )
            BitCount = 200 ;
        else
            BitCount = 160 ;
    }
    /* Non active frames */
    else if ( Ftyp == 2 )
        BitCount = 32 ;
    else
        BitCount = 2;

    for ( i = 0 ; i < BitCount ; i ++ )
        Vout[i>>3] ^= BitStream[i] << (i & 0x0007) ;

    return;
}

Word16* Par2Ser( Word32 Inp, Word16 *Pnt, int BitNum )
{
    int i   ;
    Word16  Temp ;

    for ( i = 0 ; i < BitNum ; i ++ ) {
        Temp = (Word16) Inp & (Word16)0x0001 ;
        Inp >>= 1 ;
        *Pnt ++ = Temp ;
    }

    return Pnt ;
}

/*
**
** Function:        Line_Unpk()
**
** Description:     unpacking of bitstream, gets coding parameters for a frame
**
** Links to text:   Section 4
**
** Arguments:
**
**  char   *Vinp        bitstream chars
**  Word16 *VadType
**
** Outputs:
**
**  Word16 *VadType
**
** Return value:
**
**  LINEDEF             coded parameters
**     Word16   Crc
**     Word32   LspId
**     Word16   Olp[SubFrames/2]
**     SFSDEF   Sfs[SubFrames]
**
*/
LINEDEF  Line_Unpk( char *Vinp, Word16 *Ftyp, Word16 Crc )
{
    int   i  ;
    Word16  BitStream[200] ;
    Word16 *Bsp = BitStream ;
    LINEDEF Line ;
    Word32  Temp ;
    Word16  Info;
    Word16 Bound_AcGn;

    Line.Crc = Crc;
    if(Crc != 0) return Line;

    /* Unpack the byte info to BitStream vector */
    for ( i = 0 ; i < 200 ; i ++ )
        BitStream[i] = ( Vinp[i>>3] >> (i & (Word16)0x0007) ) & (Word16)1 ;

    /* Decode the frame type and rate info */
    //Info = (Word16)Ser2Par( &Bsp, 2 ) ;
    Info = 0;
    if ( Info == 3 ) {
        *Ftyp = 0;
        Line.LspId = 0L;    /* Dummy : to avoid Borland C3.1 warning */
        return Line;
    }

    /* Decode the LspId */
    Line.LspId = Ser2Par( &Bsp, 24 ) ;

    if ( Info == 2 ) {
        /* Decode the Noise Gain */
        Line.Sfs[0].Mamp = (Word16)Ser2Par( &Bsp, 6);
        *Ftyp = 2;
        return Line ;
    }

 /*
  * Decode the common information to both rates
  */
    *Ftyp = 1;

    /* Decode the bit-rate */
    WrkRate = (Info == 0) ? Rate63 : Rate53;

    /* Decode the adaptive codebook lags */
    Temp = Ser2Par( &Bsp, 7 ) ;
    /* Test if forbidden code */
    if( Temp <= 123) {
        Line.Olp[0] = (Word16) Temp + (Word16)PitchMin ;
    }
    else {
        /* transmission error */
        Line.Crc = 1;
        return Line ;
    }

    Line.Sfs[1].AcLg = (Word16) Ser2Par( &Bsp, 2 ) ;

    Temp = Ser2Par( &Bsp, 7 ) ;
    /* Test if forbidden code */
    if( Temp <= 123) {
        Line.Olp[1] = (Word16) Temp + (Word16)PitchMin ;
    }
    else {
        /* transmission error */
        Line.Crc = 1;
        return Line ;
    }

    Line.Sfs[3].AcLg = (Word16) Ser2Par( &Bsp, 2 ) ;

    Line.Sfs[0].AcLg = 1 ;
    Line.Sfs[2].AcLg = 1 ;

    /* Decode the combined gains accordingly to the rate */
    for ( i = 0 ; i < SubFrames ; i ++ ) {

        Temp = Ser2Par( &Bsp, 12 ) ;

        Line.Sfs[i].Tran = 0 ;
        Bound_AcGn = NbFilt170 ;
        if ( (WrkRate == Rate63) && (Line.Olp[i>>1] < (SubFrLen-2) ) ) {
            Line.Sfs[i].Tran = (Word16)(Temp >> 11) ;
            Temp &= 0x000007ffL ;
            Bound_AcGn = NbFilt085 ;
        }
        Line.Sfs[i].AcGn = (Word16)(Temp / (Word16)NumOfGainLev) ;
        if(Line.Sfs[i].AcGn < Bound_AcGn ) {
            Line.Sfs[i].Mamp = (Word16)(Temp % (Word16)NumOfGainLev) ;
        }
        else {
            /* error detected */
            Line.Crc = 1;
            return Line ;
        }
    }

    /* Decode the grids */
    for ( i = 0 ; i < SubFrames ; i ++ )
        Line.Sfs[i].Grid = *Bsp ++ ;

    if (Info == 0) {

        /* Skip the reserved bit */
       // Bsp ++ ;

        /* Decode 13 bit combined position index */
       // Temp = Ser2Par( &Bsp, 13 ) ;
       // Line.Sfs[0].Ppos = ( Temp/90 ) / 9 ;
       // Line.Sfs[1].Ppos = ( Temp/90 ) % 9 ;
       // Line.Sfs[2].Ppos = ( Temp%90 ) / 9 ;
       // Line.Sfs[3].Ppos = ( Temp%90 ) % 9 ;
        Temp = Ser2Par( &Bsp, 16 ) ;
        Line.Sfs[0].Ppos = ( Temp&0xF000 )>>12  ;
        Line.Sfs[1].Ppos = ( Temp&0x0F00 )>>8 ;
        Line.Sfs[2].Ppos = ( Temp&0x00F0 )>>4 ;
        Line.Sfs[3].Ppos = ( Temp&0x000F )>>0 ;
        /* Decode all the pulse positions */
        Line.Sfs[0].Ppos = ( Line.Sfs[0].Ppos << 16 ) + Ser2Par( &Bsp, 16 ) ;
        Line.Sfs[1].Ppos = ( Line.Sfs[1].Ppos << 14 ) + Ser2Par( &Bsp, 14 ) ;
        Line.Sfs[2].Ppos = ( Line.Sfs[2].Ppos << 16 ) + Ser2Par( &Bsp, 16 ) ;
        Line.Sfs[3].Ppos = ( Line.Sfs[3].Ppos << 14 ) + Ser2Par( &Bsp, 14 ) ;

        /* Decode pulse amplitudes */
        Line.Sfs[0].Pamp = (Word16)Ser2Par( &Bsp, 6 ) ;
        Line.Sfs[1].Pamp = (Word16)Ser2Par( &Bsp, 5 ) ;
        Line.Sfs[2].Pamp = (Word16)Ser2Par( &Bsp, 6 ) ;
        Line.Sfs[3].Pamp = (Word16)Ser2Par( &Bsp, 5 ) ;
    }

    else {

        /* Decode the positions */
        for ( i = 0 ; i < SubFrames ; i ++ )
            Line.Sfs[i].Ppos = Ser2Par( &Bsp, 12 ) ;

        /* Decode the amplitudes */
        for ( i = 0 ; i < SubFrames ; i ++ )
            Line.Sfs[i].Pamp = (Word16)Ser2Par( &Bsp, 4 ) ;
    }
   return Line ;
}

Word32  Ser2Par( Word16 **Pnt, int Count )
{
    int     i ;
    Word32  Rez = 0L ;

    for ( i = 0 ; i < Count ; i ++ ) {
        Rez += (Word32) **Pnt << i ;
        (*Pnt) ++ ;
    }
    return Rez ;
}

/*
**
** Function:        Comp_En()
**
** Description:     Compute energy of a subframe vector
**
** Links to text:
**
** Arguments:
**
**  Word16 *Dpnt
**
** Outputs:         None
**
** Return value:
**
**      Word32 energy
**
*/
Word32   Comp_En( Word16 *Dpnt )
{
    int   i ;
    Word32   Rez ;
    Word16   Temp[SubFrLen] ;

    for ( i = 0 ; i < SubFrLen ; i ++ )
        Temp[i] = shr( Dpnt[i], (Word16) 2 ) ;

    Rez = (Word32) 0 ;
    for ( i = 0 ; i < SubFrLen ; i ++ )
        Rez = L_mac( Rez, Temp[i], Temp[i] ) ;

    return Rez ;
}

/*
**
** Function:        Sqrt_lbc()
**
** Description:     Square root computation
**
** Links to text:
**
** Arguments:
**
**  Word32 Num
**
** Outputs:     None
**
** Return value:
**
**  Word16 square root of num
**
*/
Word16   Sqrt_lbc( Word32 Num )
{
    int   i  ;

    Word16   Rez = (Word16) 0 ;
    Word16   Exp = (Word16) 0x4000 ;

    Word32   Acc ;

    for ( i = 0 ; i < 14 ; i ++ ) {

        Acc = L_mult( add(Rez, Exp), add(Rez, Exp) ) ;
        if ( Num >= Acc )
            Rez = add( Rez, Exp ) ;

        Exp = shr( Exp, (Word16) 1 ) ;
    }
    return Rez ;
}

/*
**
** Function:        Rand_lbc()
**
** Description:     Generator of random numbers
**
** Links to text:   Section 3.10.2
**
** Arguments:
**
**  Word16 *p
**
** Outputs:
**
**  Word16 *p
**
** Return value:
**
**  Word16 random number
**
*/
Word16   Rand_lbc( Word16 *p )
{
    Word32   Temp ;

    Temp = L_deposit_l( *p ) ;
    Temp &= (Word32) 0x0000ffff ;
    Temp = Temp*(Word32)521 + (Word32) 259 ;
    *p = extract_l( Temp ) ;
    return extract_l( Temp ) ;
}

/*
**
** Function:        Scale()
**
** Description:     Postfilter gain scaling
**
** Links to text:   Section 3.9
**
** Arguments:
**
**  Word16 *Tv
**  Word32 Sen
**
**  Inputs:
**
**  Word16 DecStat.Gain
**
** Outputs:
**
**  Word16 *Tv
**
** Return value:    None
**
*/
void  Scale( Word16 *Tv, Word32 Sen )
{
    int   i ;

    Word32   Acc0,Acc1   ;
    Word16   Exp,SfGain  ;


    Acc0 = Sen ;
    Acc1 = Comp_En( Tv ) ;

    /* Normalize both */
    if ( (Acc1 != (Word32) 0) && (Acc0 != (Word32) 0 ) ) {

        Exp = norm_l( Acc1 ) ;
        Acc1 = L_shl( Acc1, Exp ) ;

        SfGain = norm_l( Acc0 ) ;
        Acc0 = L_shl( Acc0, SfGain ) ;
        Acc0 = L_shr( Acc0, (Word16) 1 ) ;
        Exp = sub( Exp, SfGain ) ;
        Exp = add( Exp, (Word16) 1 ) ;
        Exp = sub( (Word16) 6, Exp ) ;
        if ( Exp < (Word16) 0 )
            Exp = (Word16) 0 ;

        SfGain = extract_h( Acc1 ) ;

        //SfGain = div_l( Acc0, SfGain ) ;
		SfGain = div_l( Acc0&0xFFFF0000, SfGain ) ;

        Acc0 = L_deposit_h( SfGain ) ;

        Acc0 = L_shr( Acc0, Exp ) ;

        SfGain = Sqrt_lbc( Acc0 ) ;
    }
    else
        SfGain = 0x1000 ;

    /* Filter the data */
    for ( i = 0 ; i < SubFrLen ; i ++ ) {

        /* Update gain */
        Acc0 = L_deposit_h( DecStat.Gain ) ;
        Acc0 = L_msu( Acc0, DecStat.Gain, (Word16) 0x0800 ) ;
        Acc0 = L_mac( Acc0, SfGain, (Word16) 0x0800 ) ;
        DecStat.Gain = round_2( Acc0 ) ;

        Exp = add( DecStat.Gain, shr( DecStat.Gain, (Word16) 4) ) ;

        Acc0 = L_mult( Tv[i], Exp ) ;
        Acc0 = L_shl( Acc0, (Word16) 4 ) ;
        Tv[i] = round_2( Acc0 ) ;
    }

    return;
}



