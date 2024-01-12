/**
 * TDoll 8_2 slim
 */
class TDoll extends PIXI.spine.Spine {
  constructor(skel) {
    super(skel);
    this._default_right = true;
  }

  set facingRight(right) {
    this.skeleton.flipX = this._default_right ^ right;
  }

  get facingRight() {
    return this._default_right ^ this.skeleton.flipX ? true : false;
  }
}
