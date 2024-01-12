/* *
 *  APNGBuilder
 *   Copyright 2019 by g200kg (http://www.g200kg.com/)
 * 
 *  APNGBuilder is a JS library for build APNG (Animation PNG) from PNG files.
 *	Released under the MIT License
 * */
class APNGBuilder {
  constructor(){
    this.sig = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
    this.ihdr = new Uint8Array([ 
      0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x01, 0x90, 0x00, 0x00, 0x00, 0x50,
      0x08, 0x06, 0x00, 0x00, 0x00, 0x37, 0x20, 0x23, 0xcc
    ]);
    this.actl = new Uint8Array([
      0x00, 0x00, 0x00, 0x08, 0x61, 0x63, 0x54, 0x4c, 0x00, 0x00, 0x00, 0x1f, 0x00, 0x00, 0x00, 0x00,
      0x6b, 0xfd, 0xc0, 0x43
    ]);
    this.fctl = new Uint8Array([
      0x00, 0x00, 0x00, 0x1a, 0x66, 0x63, 0x54, 0x4c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20,
      0x00, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x64, 0x03, 0xe8,
      0x01, 0x00, 0x6a, 0x3d, 0x41, 0xb6
    ]);
    this.iend = new Uint8Array([0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4e, 0x44, 0x00, 0x00, 0x00, 0x00]);
    this.frames = [];
    this.numFrames = 0;
    this.numPlays = 0;
    this.delay = 0.1;
    this.width = 0;
    this.height = 0;
  }
  setfctl(seq, width, height){
    const delaynum = (this.delay * 100)|0;
    const delayden = 100;
    this.setDwordValue(this.fctl, 8, seq);
    this.setDwordValue(this.fctl, 12, width);
    this.setDwordValue(this.fctl, 16, height);
    this.setDwordValue(this.fctl, 20, 0); // offx
    this.setDwordValue(this.fctl, 24, 0); // offy
    this.setWordValue(this.fctl, 28, delaynum);
    this.setWordValue(this.fctl, 30, delayden);
    this.setByteValue(this.fctl, 32, 1);  // dispose
    this.setByteValue(this.fctl, 33, 0);  // blend
    this.crcCalc(this.fctl, this.fctl.length);
  }
  crcInit(){
    this.crc = 0;
    this.cnt = 0;
    this.magic = 0x04c11db7;
  }
  crcCalcByte(x){
    if(this.cnt < 4){
      x ^= 0xff;
      ++this.cnt;
    }
    for(let i = 0; i < 8; ++i){
      const bit = (this.crc & 0x80000000) ? this.magic : 0;
      this.crc = (this.crc << 1) | (((x >> i) & 1) ? 1 : 0);
      this.crc ^= bit;
    }
    this.crc >>>= 0;
  }
  crcGet(){
    for(let i = 0; i < 4; ++i)
      this.crcCalcByte(0);
    this.crc =~ this.crc;
    return this.bitRev(this.crc)>>>0;
  }
  bitRev(x){
    x = (x << 16) | ((x >> 16) & 0xffff);
    x = ((x << 8) & 0xff00ff00) | ((x >> 8) & 0x00ff00ff);
    x = ((x << 4) & 0xf0f0f0f0) | ((x >> 4) & 0x0f0f0f0f);
    x = ((x << 2) & 0xcccccccc) | ((x >> 2) & 0x33333333);
    x = ((x << 1) & 0xaaaaaaaa) | ((x >> 1) & 0x55555555);
    return x;
  }
  crcCalc(ar, len){
    this.crcInit();
    for(let i = 4; i < len - 4; ++i)
      this.crcCalcByte(ar[i]);
    const ret = this.crcGet();
    ar[len - 4] = (ret >> 24) & 0xff;
    ar[len - 3] = (ret >> 16) & 0xff;
    ar[len - 2] = (ret >> 8) & 0xff;
    ar[len - 1] = ret & 0xff;
  }
  getWordValue(ar, offset){
    return ((ar[offset]<<8)|(ar[offset+1]))>>>0;
  }
  getDwordValue(ar, offset){
    return ((ar[offset]<<24)|(ar[offset+1]<<16)|(ar[offset+2]<<8)|(ar[offset+3]))>>>0;
  }
  getDwordString(ar, offset){
    return String.fromCharCode(ar[offset], ar[offset+1], ar[offset+2], ar[offset+3]);
  }
  setByteValue(ar, offset, value){
    if(typeof(value)=="undefined")
      value = 0;
    ar[offset] = value & 0xff;
  }
  setWordValue(ar, offset, value){
    if(typeof(value)=="undefined")
      value = 0;
    ar[offset] = (value>>8)&0xff;
    ar[offset + 1] = value&0xff;
  }
  setDwordValue(ar, offset, value){
    if(typeof(value)=="undefined")
      value = 0;
    ar[offset] = (value>>24)&0xff;
    ar[offset + 1] = (value>>16)&0xff;
    ar[offset + 2] = (value>>8)&0xff;
    ar[offset + 3] = value & 0xff;
  }
  setDwordString(ar, offset, str){
    ar[offset] = str.charCodeAt(0);
    ar[offset + 1] = str.charCodeAt(1);
    ar[offset + 2] = str.charCodeAt(2);
    ar[offset + 3] = str.charCodeAt(3);
  }
  readArrayBuffer(file) {
    return new Promise((resolve)=>{
      let reader = new FileReader();
      reader.onload = ()=>{
        resolve(reader.result);
      };
      reader.readAsArrayBuffer(file);
    })
  }
  getBlobFromCanvas(canvas) {
    return new Promise((resolve)=>{
      canvas.toBlob((blob)=>{
        resolve(blob);
      })
    })
  }
  extractDataChunk(ar){
    const dat = new Uint8Array(ar);
    if(dat[0]!=0x89 || dat[1]!=0x50)
      return 0;
    let offset = 8;
    let idat = [];
    let w,h;
    while(offset < dat.length){
      let size = this.getDwordValue(dat, offset);
      switch(this.getDwordString(dat, offset + 4)){
      case "IHDR":
        w = this.getDwordValue(dat, offset + 8);
        h = this.getDwordValue(dat, offset + 12);
        if(w > this.width) this.width = w;
        if(h > this.height) this.height = h;
        if(this.frames.length == 0)
          this.setDwordValue(this.ihdr, 16, this.getDwordValue(dat, offset + 16));
        break;
      case "IDAT":
        const d = dat.slice(offset, offset + 12 + size)
        idat.push({w:w, h:h, d:d});
        break;
      case "IEND":
        return idat;
      }
      offset += (size + 12);
    }
    return idat;
  }
  setNumPlays(n){
    this.numPlays = n;
  }
  setDelay(t){
    this.delay = t;
  }
  async addFrame(src){
    let ar;
    // console.log(toString.call(src));
    switch(toString.call(src)){
    case "[object Blob]":
      ar = await this.readArrayBuffer(src);
      break;
    case "[object HTMLCanvasElement]":
      ar = await this.readArrayBuffer(await this.getBlobFromCanvas(src));
      break;
    default:
      const res = await fetch(src);
      ar = await res.arrayBuffer();
    }
    this.frames.push(this.extractDataChunk(ar));
    ++this.numframe;
    this.setDwordValue(this.actl, 8, this.numframe);
  }
  /* Get APING blob */
  getAPng(){
    let len = this.sig.length + this.ihdr.length + this.actl.length + this.iend.length;
    for(let i = 0; i < this.frames.length; ++i){
      const frm = this.frames[i];
      for(let j = 0; j < frm.length; ++j){
        len += this.fctl.length;
        len += frm[j].d.length;
        if(i)
          len += 4;
      }
    }
    const ar = new Uint8Array(len);
    let offset = 0;
    ar.set(this.sig, offset);
    offset += this.sig.length;
    this.setDwordValue(this.ihdr, 8, this.width);
    this.setDwordValue(this.ihdr, 12, this.height);
    this.crcCalc(this.ihdr, this.ihdr.length);
    ar.set(this.ihdr, offset);
    offset += this.ihdr.length;
    this.setDwordValue(this.actl, 8, this.frames.length);
    this.setDwordValue(this.actl, 12, this.numPlays);
    this.crcCalc(this.actl, this.actl.length);
    ar.set(this.actl, offset);
    offset += this.actl.length;
    for(let i = 0, seq = 0; i < this.frames.length; ++i){
      const frm = this.frames[i];
      for(let j = 0; j < frm.length; ++j){
        if(j == 0){
          this.setfctl(seq++, frm[j].w, frm[j].h);
          ar.set(this.fctl, offset);
          offset += this.fctl.length;
        }
        if(i){
          const fdat = new Uint8Array(frm[j].d.length + 4);
          this.setDwordValue(fdat, 0, fdat.length-12);
          fdat.set(frm[j].d,4);
          this.setDwordString(fdat, 4, "fdAT");
          this.setDwordValue(fdat, 8, seq++);
          this.crcCalc(fdat, fdat.length);
          ar.set(fdat, offset);
          offset += fdat.length;
        }
        else{
          ar.set(frm[j].d, offset)
          offset += frm[j].d.length;
        }
      }
    }
    this.crcCalc(this.iend, this.iend.length);
    ar.set(this.iend, offset);
    return new Blob([ar]);
  }
}
