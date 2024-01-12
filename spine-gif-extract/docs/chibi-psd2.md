
# chibi-psd 0.2

- layer hidden 결정 
  - slot이 invisible 이면 전체 attachment는 히든
  - slot이 visible 이면 현재 attachment만 보이고 나머지는 hidden 

  ``` 
    slot-idx att-list
    
  ```

- [ ] gif박스 재설정
  - `setup-bg2 [cnt x0 y0 w h]`
  -  ```
     (let [bs (.getBounds d)
          [x y w h] (mapv #(aget bg (name %)) '(x y width height))]
      (setup-bg2 cntr x y w h))
     ```
     
## chibi-psd 0.2 flow 
1. [x] doll . bs CALC -> doll-bs rect  
1. [x] ADD EVERY attachment TO slotContainer 
   1. A: FIND slot idx == IDX -> GET attachment LIST 
   1. CONTAINS OF? sprites, meshes  
      1. [x] FALSE： GENERATE , ADD
         - [x] ADD TO sprites, meshes MAP 
         - [x] EXEC slotContainer addChild , SET visible false
         - WARN：~~RISK: mesh?? slotContainer transform crash~~
      1. [x] YES： REST attachment JUMP TO A:             
1. [x] EVERY slotContainer.visible HOLD -> slot-vflag-lst
   1. [x] EVERY children.visible HOLD -> slot-ko-vflag-lst  
1. [x] EVERY slotContainer children. visible SET AS false 
1. slotContainer.map child.map -> part
   1. [x] part.visible SET AS true 
   1. [x] CALC part.bounds -> obj-bs
   1. [x] capture
   1. [x] psd layer GENERATE -> layer
   1. [x] top left MODIFY
   1. [x] layer <- image-data SET
      * CASE slot-vflag == false　OR slot-ko-vflag == false
          - layer.hidden = t 
      * OTHERWISE
          - layer.hidden = FALSE
   1. layer -> psd children ADD 
1. psd SAVE
1. slot-vflag-lst slot-ko-vflag-lst LOOPS visible RESTORE
1. **END** 🙃  