
## Spine3.8 에서 프리코네 캐릭터가 파츠가 사라지는 현상

- [ ] bone의 matrix 가 깨지고 있음.
- [ ] scale timeline curves 에 NaN이 들어가 있다.
  ```
    [2, NaN, 0.02800000086426735, NaN, 0.10400000214576721, NaN ... 
  ```
    matrix ,scaleX, scaleY가 NaN이 되면서 화면에서 안 그려지게 됨.
- [ ] 타임라인 curve에 'linear'가 있었음
   - [ ] linear의 경우 curve 프로퍼티가 아예 없어야 함
   
- [ ] 프리코네 Spine 보정

    ```javascript
    // slot 
    var regionX = slotX - regionOrigW/2 + regionOffsetX;
    var regionY = slotY - regionOrigH/2 + regionOffsetY;
    var newSlotX = regionX + regionW/2;
    var newSlotY = regionY + regionH/2;
    
    // region attachment
    var regionOrigW = regionW;
    var regionOrigH = regionH;
    var regionOffsetX = 0;
    var regionOffsetY = 0;
    
    ```   


## pixi-spine 이 premultipliedAlpha 가 spine-ts 와 다른 이유 


- pixi-spine 은 attachment 별 premultiplied가 설정되지 않음
 
- spine-ts 는 설정이 있음.  render시 t/f여부도 확인 
 
- pixi-spine은 atlas로 읽어올때 basetexture 통으로 설정
    *  따라서 개별 텍스처로 분해해야 한다
    *  애초에 양쪽이 render시 computerWorldVerties 에 color 계산 여부가 다름 
    
    
     
---

발할라 victory-victoryloop 샷 지오

``   
"[["Jill",578,750],["Dorothy",287,750],["Dana",674,750],["Alma",415,750],["SEI",850,750],["Stella",994,750]]"
``


---

---