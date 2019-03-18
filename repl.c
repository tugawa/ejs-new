#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/types.h>

#define BUFLEN 1024
#define R (0)
#define W (1)

int makeJSFile(char buf[]){
    FILE *fp;
    int i;
    
    if((fp=fopen("document/tmp.js", "w"))==NULL){
        fprintf(stderr, "file open faild: tmp.js\n");
        return -1;
    }
    
    for(i=0;i<BUFLEN;i++)
        if(buf[i]=='\n'){
            buf[i]='\0';
            break;
        }
    
    //fprintf(fp,"%s",buf);
    fprintf(fp,"it = (%s)",buf);
    
    fclose(fp);
    
    return 0;
}

int main(int argc, char *argv[]){
    char buf[BUFLEN], output[BUFLEN],compilecommand[BUFLEN];
    int i,j;
    int fd_r, fd_w;
    int p_id, pipe_p2c[2], pipe_c2p[2], status;
    int fn = 0;
    FILE *fp;
    int test;
    
    char *vm_place = "/Users/obayashi_kenzo/ejs/git/ejsvm/gasshuku2019-ume-obayashi/obc_test/ejsvm";
    char *c_place =  "/Users/obayashi_kenzo/ejs/git/ejsc/gasshuku2019-matsu/newejsc.jar";
    
    if(pipe(pipe_c2p)<0){
        perror("popen c2p");
        return -1;
    }
    if(pipe(pipe_p2c)<0){
        perror("popen p2c");
        close(pipe_c2p[R]);
        close(pipe_c2p[W]);
        return -1;
    }
    
    if((p_id=fork())==0){
        close(pipe_p2c[W]);  close(pipe_c2p[R]);
        dup2(pipe_p2c[R],0); dup2(pipe_c2p[W],1);
        close(pipe_p2c[R]);  close(pipe_c2p[W]);
        execl(vm_place, vm_place, "-R", NULL);
        return -1;
    }
    
    close(pipe_p2c[R]); close(pipe_c2p[W]);
    fd_w=pipe_p2c[W]; fd_r=pipe_c2p[R];
    
    for(int i = 0;;i++){
        if(i%2==1){ continue; }
        printf("eJSrepl > ");
        fgets(buf,BUFLEN,stdin);
        if(strcmp(buf,"exit\n")==0) break;
        //printf("test");
        //fprintf(fd_w, "%s", buf);
        if(makeJSFile(buf)<0) return -1;
        
        sprintf(compilecommand, "java -jar %s -fn %d document/tmp.js -opt-g3 -opt-const -opt-rie -opt-cce -opt-reg -opt-copy -opt-si super-inst.spec --out-obc instructions.def", c_place, fn);
        system(compilecommand);
        
        if((fp=fopen("document/tmp.obc", "rb"))==NULL){
            fprintf(stderr, "error: fileopen\n");
            return -1;
        }
        
        //printf("b");
        
        unsigned char tmp[2];
        fread(tmp, sizeof(unsigned char), 2, fp);
        fn += tmp[0]*256+tmp[1];
        //printf("%02x %02x ", tmp[0], tmp[1]);
        write(fd_w, tmp, sizeof(unsigned char)*2);
        
        while((test=fread(tmp, sizeof(unsigned char), 1, fp))==1){
            //printf("%02x %02x ", tmp[0], tmp[1]);
            write(fd_w, tmp, sizeof(unsigned char));
            //printf("a");
        }
        
        printf("it = ");
        while(read(fd_r, output, sizeof(char))<1);
        do
            printf("%s", output);
        while(read(fd_r, output, sizeof(char))>0 && output[0]!='\n');
        
        printf("\n\n");
        
        fclose(fp);
    }
    
    return 0;
}
