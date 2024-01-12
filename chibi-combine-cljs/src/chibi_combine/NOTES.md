


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


## 2020.09.01 

Non essential 설정된 mesh는 edges, width, height 정보가 없어서
DragonBones에서 제대로 로딩이 안 된다.


## 2020.11.15

### Spine 2 vs 3 변환 

- bone 

| | 2.x | 3.x |
---|-----|-----
|flipX | O | 없음.
| matrix | m00 | matrix.a
|        | m10 | matrix.b
|        | m01 | matrix.c 
|        | m11 | matrix.d 
| 회전(deg) | rotation | rotation 
 






..
