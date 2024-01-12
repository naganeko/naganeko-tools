
// class TDLink extends PIXI.projection.Container2d {
// class TDGroup extends PIXI.spine.Spine {
class TDGroup extends PIXI.Container {
  constructor() {
    super();
    // super({bones:[], slots:[],ikConstraints:[]});
    // this._default_right = true;
    this._init2();
  }

  _init2() {
    this.skeleton = {
      setToSetupPose : function () {
        this.dolls
          .map(d => d.skeleton)
          .forEach(d => d.setToSetupPose());
      }.bind(this)
    }

    this.state = {
      setAnimationByName: function (trackIndex, animationName, loop, opt) {
        this.dolls
          .map(d => d.state)
          .forEach(d => d.setAnimationByName(trackIndex, animationName, loop, opt));
      }.bind(this)

      ,addAnimationByName: function (trackIndex, animationName, loop, opt) {
        this.dolls
          .map(d => d.state)
          .forEach(d => d.addAnimationByName(trackIndex, animationName, loop, opt));
      }.bind(this)

      ,clearTracks: function () {
        this.dolls
          .map(d => d.state)
          .forEach(d => d.clearTracks())
          // .forEach(d => { console.log(d, d.skin);})
        ;
      }.bind(this)
    };
  }
  // set facingRight(right) {
  //   this.skeleton.flipX = this._default_right ^ right;
  // }
  //
  // get facingRight() {
  //   return this._default_right ^ this.skeleton.flipX ? true : false;
  // }

  get dolls() {
    return this.children;
  }

  set autoUpdate(flag) {
    // super.autoUpdate = flag;
    this.dolls.forEach(d => d.autoUpdate = flag);
  }

  update(delta) {
    this.dolls.forEach(d => d.update(delta));
  }


}

TDGroup.selectedFilters =  [new PIXI.filters.GlowFilter(10,3,0,0x88ff88,0.5)];
TDGroup.unselectedFilters =  [];

