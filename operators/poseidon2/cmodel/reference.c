/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Poseidon2 Hardware Accelerator
 * Chisel hardware for the fixed 12-element Goldilocks Poseidon2 permutation.
 * Repository: https://github.com/OpenRTL-Lab/poseidon2-goldilocks
 * File: operators/poseidon2/cmodel/reference.c
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include "parameters.h"
static const uint64_t P = UINT64_C(0xffffffff00000001);
static uint64_t add(uint64_t a,uint64_t b){return (uint64_t)(((__uint128_t)a+b)%P);}
static uint64_t mul(uint64_t a,uint64_t b){return (uint64_t)(((__uint128_t)a*b)%P);}
static uint64_t power7(uint64_t x){
  uint64_t a=mul(x,x),b=mul(a,x),c=mul(a,a);return mul(b,c);
}
static void external(uint64_t s[12]){
  /* 直接矩阵点积，不复制硬件加法链。 */
  static const unsigned m[4][4]={{2,3,1,1},{1,2,3,1},{1,1,2,3},{3,1,1,2}};
  uint64_t v[12]={0},sum[4]={0};
  for(unsigned b=0;b<3;b++)for(unsigned i=0;i<4;i++)
    for(unsigned j=0;j<4;j++)v[4*b+i]=add(v[4*b+i],mul(m[i][j],s[4*b+j]));
  for(unsigned i=0;i<12;i++)sum[i%4]=add(sum[i%4],v[i]);
  for(unsigned i=0;i<12;i++)s[i]=add(v[i],sum[i%4]);
}
static void permutation(uint64_t s[12]){
  external(s);
  for(unsigned r=0;r<30;r++){
    if(r<4||r>=26){for(unsigned i=0;i<12;i++)s[i]=power7(add(s[i],RC[r][i]));external(s);}
    else{
      s[0]=power7(add(s[0],RC[r][0]));
      uint64_t sum=0;for(unsigned i=0;i<12;i++)sum=add(sum,s[i]);
      for(unsigned i=0;i<12;i++)s[i]=add(sum,mul(s[i],DIAG[i]));
    }
  }
}
static uint64_t rng=UINT64_C(0x58c913df702ba465);
static uint64_t random_word(void){
  rng^=rng<<13;rng^=rng>>7;rng^=rng<<17;return rng%P;
}
int main(int argc,char **argv){
  uint64_t kat[12];for(unsigned i=0;i<12;i++)kat[i]=i;permutation(kat);
  for(unsigned i=0;i<12;i++)if(kat[i]!=KAT[i]){fprintf(stderr,"KAT mismatch %u\n",i);return 2;}
  if(argc!=3)return 3;
  unsigned n=(unsigned)strtoul(argv[2],NULL,10);if(n<1||n>4096)return 4;
  char path[4096];snprintf(path,sizeof(path),"%s/input.hex",argv[1]);FILE *in=fopen(path,"wx");
  snprintf(path,sizeof(path),"%s/expected.hex",argv[1]);FILE *out=fopen(path,"wx");
  if(!in||!out)return 5;
  for(unsigned t=0;t<n;t++){
    uint64_t s[12];for(unsigned i=0;i<12;i++){
      s[i]=t==0?i:t==1?0:t==2?P-1:t==3?1:t==4?(i%2?P-1:0):random_word();
      fprintf(in,"%016" PRIx64 "\n",s[i]);
    }
    permutation(s);for(unsigned i=0;i<12;i++)fprintf(out,"%016" PRIx64 "\n",s[i]);
  }
  if(fclose(in)||fclose(out))return 6;
  printf("REFERENCE kat=1 tasks=%u words=%u\n",n,12*n);return 0;
}
