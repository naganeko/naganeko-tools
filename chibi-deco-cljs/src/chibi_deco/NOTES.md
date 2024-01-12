


## pixi-spine 이 premultipliedAlpha 가 spine-ts 와 다른 이유 


- pixi-spine 은 attachment 별 premultiplied가 설정되지 않음
 
- spine-ts 는 설정이 있음.  render시 t/f여부도 확인 
 
- pixi-spine은 atlas로 읽어올때 basetexture 통으로 설정
    *  따라서 개별 텍스처로 분해해야 한다
    *  애초에 양쪽이 render시 computerWorldVerties 에 color 계산 여부가 다름 
    
    
     