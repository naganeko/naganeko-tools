/*!
 * pixi-filters - v5.0.0
 * Compiled Fri, 23 Sep 2022 20:48:56 UTC
 *
 * pixi-filters is licensed under the MIT License.
 * http://www.opensource.org/licenses/mit-license
 */var __filters=function(n,o,H,b){"use strict";var J=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,ee=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;

uniform float gamma;
uniform float contrast;
uniform float saturation;
uniform float brightness;
uniform float red;
uniform float green;
uniform float blue;
uniform float alpha;

void main(void)
{
    vec4 c = texture2D(uSampler, vTextureCoord);

    if (c.a > 0.0) {
        c.rgb /= c.a;

        vec3 rgb = pow(c.rgb, vec3(1. / gamma));
        rgb = mix(vec3(.5), mix(vec3(dot(vec3(.2125, .7154, .0721), rgb)), rgb, saturation), contrast);
        rgb.r *= red;
        rgb.g *= green;
        rgb.b *= blue;
        c.rgb = rgb * brightness;

        c.rgb *= c.a;
    }

    gl_FragColor = c * alpha;
}
`;class te extends o.Filter{constructor(e){super(J,ee),this.gamma=1,this.saturation=1,this.contrast=1,this.brightness=1,this.red=1,this.green=1,this.blue=1,this.alpha=1,Object.assign(this,e)}apply(e,r,i,s){this.uniforms.gamma=Math.max(this.gamma,1e-4),this.uniforms.saturation=this.saturation,this.uniforms.contrast=this.contrast,this.uniforms.brightness=this.brightness,this.uniforms.red=this.red,this.uniforms.green=this.green,this.uniforms.blue=this.blue,this.uniforms.alpha=this.alpha,e.applyFilter(this,r,i,s)}}var re=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,ie=`
varying vec2 vTextureCoord;
uniform sampler2D uSampler;

uniform vec2 uOffset;

void main(void)
{
    vec4 color = vec4(0.0);

    // Sample top left pixel
    color += texture2D(uSampler, vec2(vTextureCoord.x - uOffset.x, vTextureCoord.y + uOffset.y));

    // Sample top right pixel
    color += texture2D(uSampler, vec2(vTextureCoord.x + uOffset.x, vTextureCoord.y + uOffset.y));

    // Sample bottom right pixel
    color += texture2D(uSampler, vec2(vTextureCoord.x + uOffset.x, vTextureCoord.y - uOffset.y));

    // Sample bottom left pixel
    color += texture2D(uSampler, vec2(vTextureCoord.x - uOffset.x, vTextureCoord.y - uOffset.y));

    // Average
    color *= 0.25;

    gl_FragColor = color;
}`,oe=`
varying vec2 vTextureCoord;
uniform sampler2D uSampler;

uniform vec2 uOffset;
uniform vec4 filterClamp;

void main(void)
{
    vec4 color = vec4(0.0);

    // Sample top left pixel
    color += texture2D(uSampler, clamp(vec2(vTextureCoord.x - uOffset.x, vTextureCoord.y + uOffset.y), filterClamp.xy, filterClamp.zw));

    // Sample top right pixel
    color += texture2D(uSampler, clamp(vec2(vTextureCoord.x + uOffset.x, vTextureCoord.y + uOffset.y), filterClamp.xy, filterClamp.zw));

    // Sample bottom right pixel
    color += texture2D(uSampler, clamp(vec2(vTextureCoord.x + uOffset.x, vTextureCoord.y - uOffset.y), filterClamp.xy, filterClamp.zw));

    // Sample bottom left pixel
    color += texture2D(uSampler, clamp(vec2(vTextureCoord.x - uOffset.x, vTextureCoord.y - uOffset.y), filterClamp.xy, filterClamp.zw));

    // Average
    color *= 0.25;

    gl_FragColor = color;
}
`;class h extends o.Filter{constructor(e=4,r=3,i=!1){super(re,i?oe:ie),this._kernels=[],this._blur=4,this._quality=3,this.uniforms.uOffset=new Float32Array(2),this._pixelSize=new o.Point,this.pixelSize=1,this._clamp=i,Array.isArray(e)?this.kernels=e:(this._blur=e,this.quality=r)}apply(e,r,i,s){const a=this._pixelSize.x/r._frame.width,l=this._pixelSize.y/r._frame.height;let u;if(this._quality===1||this._blur===0)u=this._kernels[0]+.5,this.uniforms.uOffset[0]=u*a,this.uniforms.uOffset[1]=u*l,e.applyFilter(this,r,i,s);else{const c=e.getFilterTexture();let d=r,m=c,g;const Q=this._quality-1;for(let _=0;_<Q;_++)u=this._kernels[_]+.5,this.uniforms.uOffset[0]=u*a,this.uniforms.uOffset[1]=u*l,e.applyFilter(this,d,m,1),g=d,d=m,m=g;u=this._kernels[Q]+.5,this.uniforms.uOffset[0]=u*a,this.uniforms.uOffset[1]=u*l,e.applyFilter(this,d,i,s),e.returnFilterTexture(c)}}_updatePadding(){this.padding=Math.ceil(this._kernels.reduce((e,r)=>e+r+.5,0))}_generateKernels(){const e=this._blur,r=this._quality,i=[e];if(e>0){let s=e;const a=e/r;for(let l=1;l<r;l++)s-=a,i.push(s)}this._kernels=i,this._updatePadding()}get kernels(){return this._kernels}set kernels(e){Array.isArray(e)&&e.length>0?(this._kernels=e,this._quality=e.length,this._blur=Math.max(...e)):(this._kernels=[0],this._quality=1)}get clamp(){return this._clamp}set pixelSize(e){typeof e=="number"?(this._pixelSize.x=e,this._pixelSize.y=e):Array.isArray(e)?(this._pixelSize.x=e[0],this._pixelSize.y=e[1]):e instanceof o.Point?(this._pixelSize.x=e.x,this._pixelSize.y=e.y):(this._pixelSize.x=1,this._pixelSize.y=1)}get pixelSize(){return this._pixelSize}get quality(){return this._quality}set quality(e){this._quality=Math.max(1,Math.round(e)),this._generateKernels()}get blur(){return this._blur}set blur(e){this._blur=e,this._generateKernels()}}var S=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,se=`
uniform sampler2D uSampler;
varying vec2 vTextureCoord;

uniform float threshold;

void main() {
    vec4 color = texture2D(uSampler, vTextureCoord);

    // A simple & fast algorithm for getting brightness.
    // It's inaccuracy , but good enought for this feature.
    float _max = max(max(color.r, color.g), color.b);
    float _min = min(min(color.r, color.g), color.b);
    float brightness = (_max + _min) * 0.5;

    if(brightness > threshold) {
        gl_FragColor = color;
    } else {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    }
}
`;class ae extends o.Filter{constructor(e=.5){super(S,se),this.threshold=e}get threshold(){return this.uniforms.threshold}set threshold(e){this.uniforms.threshold=e}}var le=`uniform sampler2D uSampler;
varying vec2 vTextureCoord;

uniform sampler2D bloomTexture;
uniform float bloomScale;
uniform float brightness;

void main() {
    vec4 color = texture2D(uSampler, vTextureCoord);
    color.rgb *= brightness;
    vec4 bloomColor = vec4(texture2D(bloomTexture, vTextureCoord).rgb, 0.0);
    bloomColor.rgb *= bloomScale;
    gl_FragColor = color + bloomColor;
}
`;const T=class extends o.Filter{constructor(t){super(S,le),this.bloomScale=1,this.brightness=1,this._resolution=o.settings.FILTER_RESOLUTION,typeof t=="number"&&(t={threshold:t});const e=Object.assign(T.defaults,t);this.bloomScale=e.bloomScale,this.brightness=e.brightness;const{kernels:r,blur:i,quality:s,pixelSize:a,resolution:l}=e;this._extractFilter=new ae(e.threshold),this._extractFilter.resolution=l,this._blurFilter=r?new h(r):new h(i,s),this.pixelSize=a,this.resolution=l}apply(t,e,r,i,s){const a=t.getFilterTexture();this._extractFilter.apply(t,e,a,1,s);const l=t.getFilterTexture();this._blurFilter.apply(t,a,l,1),this.uniforms.bloomScale=this.bloomScale,this.uniforms.brightness=this.brightness,this.uniforms.bloomTexture=l,t.applyFilter(this,e,r,i),t.returnFilterTexture(l),t.returnFilterTexture(a)}get resolution(){return this._resolution}set resolution(t){this._resolution=t,this._extractFilter&&(this._extractFilter.resolution=t),this._blurFilter&&(this._blurFilter.resolution=t)}get threshold(){return this._extractFilter.threshold}set threshold(t){this._extractFilter.threshold=t}get kernels(){return this._blurFilter.kernels}set kernels(t){this._blurFilter.kernels=t}get blur(){return this._blurFilter.blur}set blur(t){this._blurFilter.blur=t}get quality(){return this._blurFilter.quality}set quality(t){this._blurFilter.quality=t}get pixelSize(){return this._blurFilter.pixelSize}set pixelSize(t){this._blurFilter.pixelSize=t}};let F=T;F.defaults={threshold:.5,bloomScale:1,brightness:1,kernels:null,blur:8,quality:4,pixelSize:1,resolution:o.settings.FILTER_RESOLUTION};var ne=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,ue=`varying vec2 vTextureCoord;

uniform vec4 filterArea;
uniform float pixelSize;
uniform sampler2D uSampler;

vec2 mapCoord( vec2 coord )
{
    coord *= filterArea.xy;
    coord += filterArea.zw;

    return coord;
}

vec2 unmapCoord( vec2 coord )
{
    coord -= filterArea.zw;
    coord /= filterArea.xy;

    return coord;
}

vec2 pixelate(vec2 coord, vec2 size)
{
    return floor( coord / size ) * size;
}

vec2 getMod(vec2 coord, vec2 size)
{
    return mod( coord , size) / size;
}

float character(float n, vec2 p)
{
    p = floor(p*vec2(4.0, -4.0) + 2.5);

    if (clamp(p.x, 0.0, 4.0) == p.x)
    {
        if (clamp(p.y, 0.0, 4.0) == p.y)
        {
            if (int(mod(n/exp2(p.x + 5.0*p.y), 2.0)) == 1) return 1.0;
        }
    }
    return 0.0;
}

void main()
{
    vec2 coord = mapCoord(vTextureCoord);

    // get the rounded color..
    vec2 pixCoord = pixelate(coord, vec2(pixelSize));
    pixCoord = unmapCoord(pixCoord);

    vec4 color = texture2D(uSampler, pixCoord);

    // determine the character to use
    float gray = (color.r + color.g + color.b) / 3.0;

    float n =  65536.0;             // .
    if (gray > 0.2) n = 65600.0;    // :
    if (gray > 0.3) n = 332772.0;   // *
    if (gray > 0.4) n = 15255086.0; // o
    if (gray > 0.5) n = 23385164.0; // &
    if (gray > 0.6) n = 15252014.0; // 8
    if (gray > 0.7) n = 13199452.0; // @
    if (gray > 0.8) n = 11512810.0; // #

    // get the mod..
    vec2 modd = getMod(coord, vec2(pixelSize));

    gl_FragColor = color * character( n, vec2(-1.0) + modd * 2.0);

}
`;class ce extends o.Filter{constructor(e=8){super(ne,ue),this.size=e}get size(){return this.uniforms.pixelSize}set size(e){this.uniforms.pixelSize=e}}var fe=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,de=`precision mediump float;

varying vec2 vTextureCoord;
uniform sampler2D uSampler;
uniform vec4 filterArea;

uniform float transformX;
uniform float transformY;
uniform vec3 lightColor;
uniform float lightAlpha;
uniform vec3 shadowColor;
uniform float shadowAlpha;

void main(void) {
    vec2 transform = vec2(1.0 / filterArea) * vec2(transformX, transformY);
    vec4 color = texture2D(uSampler, vTextureCoord);
    float light = texture2D(uSampler, vTextureCoord - transform).a;
    float shadow = texture2D(uSampler, vTextureCoord + transform).a;

    color.rgb = mix(color.rgb, lightColor, clamp((color.a - light) * lightAlpha, 0.0, 1.0));
    color.rgb = mix(color.rgb, shadowColor, clamp((color.a - shadow) * shadowAlpha, 0.0, 1.0));
    gl_FragColor = vec4(color.rgb * color.a, color.a);
}
`;class he extends o.Filter{constructor(e){super(fe,de),this._thickness=2,this._angle=0,this.uniforms.lightColor=new Float32Array(3),this.uniforms.shadowColor=new Float32Array(3),Object.assign(this,{rotation:45,thickness:2,lightColor:16777215,lightAlpha:.7,shadowColor:0,shadowAlpha:.7},e),this.padding=1}_updateTransform(){this.uniforms.transformX=this._thickness*Math.cos(this._angle),this.uniforms.transformY=this._thickness*Math.sin(this._angle)}get rotation(){return this._angle/o.DEG_TO_RAD}set rotation(e){this._angle=e*o.DEG_TO_RAD,this._updateTransform()}get thickness(){return this._thickness}set thickness(e){this._thickness=e,this._updateTransform()}get lightColor(){return o.utils.rgb2hex(this.uniforms.lightColor)}set lightColor(e){o.utils.hex2rgb(e,this.uniforms.lightColor)}get lightAlpha(){return this.uniforms.lightAlpha}set lightAlpha(e){this.uniforms.lightAlpha=e}get shadowColor(){return o.utils.rgb2hex(this.uniforms.shadowColor)}set shadowColor(e){o.utils.hex2rgb(e,this.uniforms.shadowColor)}get shadowAlpha(){return this.uniforms.shadowAlpha}set shadowAlpha(e){this.uniforms.shadowAlpha=e}}class me extends o.Filter{constructor(e=2,r=4,i=o.settings.FILTER_RESOLUTION,s=5){super();let a,l;typeof e=="number"?(a=e,l=e):e instanceof o.Point?(a=e.x,l=e.y):Array.isArray(e)&&(a=e[0],l=e[1]),this.blurXFilter=new b.BlurFilterPass(!0,a,r,i,s),this.blurYFilter=new b.BlurFilterPass(!1,l,r,i,s),this.blurYFilter.blendMode=o.BLEND_MODES.SCREEN,this.defaultFilter=new H.AlphaFilter}apply(e,r,i,s){const a=e.getFilterTexture();this.defaultFilter.apply(e,r,i,s),this.blurXFilter.apply(e,r,a,1),this.blurYFilter.apply(e,a,i,0),e.returnFilterTexture(a)}get blur(){return this.blurXFilter.blur}set blur(e){this.blurXFilter.blur=this.blurYFilter.blur=e}get blurX(){return this.blurXFilter.blur}set blurX(e){this.blurXFilter.blur=e}get blurY(){return this.blurYFilter.blur}set blurY(e){this.blurYFilter.blur=e}}var ve=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,ge=`uniform float radius;
uniform float strength;
uniform vec2 center;
uniform sampler2D uSampler;
varying vec2 vTextureCoord;

uniform vec4 filterArea;
uniform vec4 filterClamp;
uniform vec2 dimensions;

void main()
{
    vec2 coord = vTextureCoord * filterArea.xy;
    coord -= center * dimensions.xy;
    float distance = length(coord);
    if (distance < radius) {
        float percent = distance / radius;
        if (strength > 0.0) {
            coord *= mix(1.0, smoothstep(0.0, radius / distance, percent), strength * 0.75);
        } else {
            coord *= mix(1.0, pow(percent, 1.0 + strength * 0.75) * radius / distance, 1.0 - percent);
        }
    }
    coord += center * dimensions.xy;
    coord /= filterArea.xy;
    vec2 clampedCoord = clamp(coord, filterClamp.xy, filterClamp.zw);
    vec4 color = texture2D(uSampler, clampedCoord);
    if (coord != clampedCoord) {
        color *= max(0.0, 1.0 - length(coord - clampedCoord));
    }

    gl_FragColor = color;
}
`;const z=class extends o.Filter{constructor(t){super(ve,ge),this.uniforms.dimensions=new Float32Array(2),Object.assign(this,z.defaults,t)}apply(t,e,r,i){const{width:s,height:a}=e.filterFrame;this.uniforms.dimensions[0]=s,this.uniforms.dimensions[1]=a,t.applyFilter(this,e,r,i)}get radius(){return this.uniforms.radius}set radius(t){this.uniforms.radius=t}get strength(){return this.uniforms.strength}set strength(t){this.uniforms.strength=t}get center(){return this.uniforms.center}set center(t){this.uniforms.center=t}};let A=z;A.defaults={center:[.5,.5],radius:100,strength:1};var xe=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,pe=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;
uniform sampler2D colorMap;
uniform float _mix;
uniform float _size;
uniform float _sliceSize;
uniform float _slicePixelSize;
uniform float _sliceInnerSize;
void main() {
    vec4 color = texture2D(uSampler, vTextureCoord.xy);

    vec4 adjusted;
    if (color.a > 0.0) {
        color.rgb /= color.a;
        float innerWidth = _size - 1.0;
        float zSlice0 = min(floor(color.b * innerWidth), innerWidth);
        float zSlice1 = min(zSlice0 + 1.0, innerWidth);
        float xOffset = _slicePixelSize * 0.5 + color.r * _sliceInnerSize;
        float s0 = xOffset + (zSlice0 * _sliceSize);
        float s1 = xOffset + (zSlice1 * _sliceSize);
        float yOffset = _sliceSize * 0.5 + color.g * (1.0 - _sliceSize);
        vec4 slice0Color = texture2D(colorMap, vec2(s0,yOffset));
        vec4 slice1Color = texture2D(colorMap, vec2(s1,yOffset));
        float zOffset = fract(color.b * innerWidth);
        adjusted = mix(slice0Color, slice1Color, zOffset);

        color.rgb *= color.a;
    }
    gl_FragColor = vec4(mix(color, adjusted, _mix).rgb, color.a);

}`;class ye extends o.Filter{constructor(e,r=!1,i=1){super(xe,pe),this.mix=1,this._size=0,this._sliceSize=0,this._slicePixelSize=0,this._sliceInnerSize=0,this._nearest=!1,this._scaleMode=null,this._colorMap=null,this._scaleMode=null,this.nearest=r,this.mix=i,this.colorMap=e}apply(e,r,i,s){this.uniforms._mix=this.mix,e.applyFilter(this,r,i,s)}get colorSize(){return this._size}get colorMap(){return this._colorMap}set colorMap(e){!e||(e instanceof o.Texture||(e=o.Texture.from(e)),e!=null&&e.baseTexture&&(e.baseTexture.scaleMode=this._scaleMode,e.baseTexture.mipmap=o.MIPMAP_MODES.OFF,this._size=e.height,this._sliceSize=1/this._size,this._slicePixelSize=this._sliceSize/this._size,this._sliceInnerSize=this._slicePixelSize*(this._size-1),this.uniforms._size=this._size,this.uniforms._sliceSize=this._sliceSize,this.uniforms._slicePixelSize=this._slicePixelSize,this.uniforms._sliceInnerSize=this._sliceInnerSize,this.uniforms.colorMap=e),this._colorMap=e)}get nearest(){return this._nearest}set nearest(e){this._nearest=e,this._scaleMode=e?o.SCALE_MODES.NEAREST:o.SCALE_MODES.LINEAR;const r=this._colorMap;r&&r.baseTexture&&(r.baseTexture._glTextures={},r.baseTexture.scaleMode=this._scaleMode,r.baseTexture.mipmap=o.MIPMAP_MODES.OFF,r._updateID++,r.baseTexture.emit("update",r.baseTexture))}updateColorMap(){const e=this._colorMap;e&&e.baseTexture&&(e._updateID++,e.baseTexture.emit("update",e.baseTexture),this.colorMap=e)}destroy(e=!1){this._colorMap&&this._colorMap.destroy(e),super.destroy()}}var Ce=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,_e=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;
uniform vec3 color;
uniform float alpha;

void main(void) {
    vec4 currentColor = texture2D(uSampler, vTextureCoord);
    gl_FragColor = vec4(mix(currentColor.rgb, color.rgb, currentColor.a * alpha), currentColor.a);
}
`;class be extends o.Filter{constructor(e=0,r=1){super(Ce,_e),this._color=0,this._alpha=1,this.uniforms.color=new Float32Array(3),this.color=e,this.alpha=r}set color(e){const r=this.uniforms.color;typeof e=="number"?(o.utils.hex2rgb(e,r),this._color=e):(r[0]=e[0],r[1]=e[1],r[2]=e[2],this._color=o.utils.rgb2hex(r))}get color(){return this._color}set alpha(e){this.uniforms.alpha=e,this._alpha=e}get alpha(){return this._alpha}}var Se=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Te=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;
uniform vec3 originalColor;
uniform vec3 newColor;
uniform float epsilon;
void main(void) {
    vec4 currentColor = texture2D(uSampler, vTextureCoord);
    vec3 colorDiff = originalColor - (currentColor.rgb / max(currentColor.a, 0.0000000001));
    float colorDistance = length(colorDiff);
    float doReplace = step(colorDistance, epsilon);
    gl_FragColor = vec4(mix(currentColor.rgb, (newColor + colorDiff) * currentColor.a, doReplace), currentColor.a);
}
`;class Fe extends o.Filter{constructor(e=16711680,r=0,i=.4){super(Se,Te),this._originalColor=16711680,this._newColor=0,this.uniforms.originalColor=new Float32Array(3),this.uniforms.newColor=new Float32Array(3),this.originalColor=e,this.newColor=r,this.epsilon=i}set originalColor(e){const r=this.uniforms.originalColor;typeof e=="number"?(o.utils.hex2rgb(e,r),this._originalColor=e):(r[0]=e[0],r[1]=e[1],r[2]=e[2],this._originalColor=o.utils.rgb2hex(r))}get originalColor(){return this._originalColor}set newColor(e){const r=this.uniforms.newColor;typeof e=="number"?(o.utils.hex2rgb(e,r),this._newColor=e):(r[0]=e[0],r[1]=e[1],r[2]=e[2],this._newColor=o.utils.rgb2hex(r))}get newColor(){return this._newColor}set epsilon(e){this.uniforms.epsilon=e}get epsilon(){return this.uniforms.epsilon}}var ze=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Ae=`precision mediump float;

varying mediump vec2 vTextureCoord;

uniform sampler2D uSampler;
uniform vec2 texelSize;
uniform float matrix[9];

void main(void)
{
   vec4 c11 = texture2D(uSampler, vTextureCoord - texelSize); // top left
   vec4 c12 = texture2D(uSampler, vec2(vTextureCoord.x, vTextureCoord.y - texelSize.y)); // top center
   vec4 c13 = texture2D(uSampler, vec2(vTextureCoord.x + texelSize.x, vTextureCoord.y - texelSize.y)); // top right

   vec4 c21 = texture2D(uSampler, vec2(vTextureCoord.x - texelSize.x, vTextureCoord.y)); // mid left
   vec4 c22 = texture2D(uSampler, vTextureCoord); // mid center
   vec4 c23 = texture2D(uSampler, vec2(vTextureCoord.x + texelSize.x, vTextureCoord.y)); // mid right

   vec4 c31 = texture2D(uSampler, vec2(vTextureCoord.x - texelSize.x, vTextureCoord.y + texelSize.y)); // bottom left
   vec4 c32 = texture2D(uSampler, vec2(vTextureCoord.x, vTextureCoord.y + texelSize.y)); // bottom center
   vec4 c33 = texture2D(uSampler, vTextureCoord + texelSize); // bottom right

   gl_FragColor =
       c11 * matrix[0] + c12 * matrix[1] + c13 * matrix[2] +
       c21 * matrix[3] + c22 * matrix[4] + c23 * matrix[5] +
       c31 * matrix[6] + c32 * matrix[7] + c33 * matrix[8];

   gl_FragColor.a = c22.a;
}
`;class Pe extends o.Filter{constructor(e,r=200,i=200){super(ze,Ae),this.uniforms.texelSize=new Float32Array(2),this.uniforms.matrix=new Float32Array(9),e!==void 0&&(this.matrix=e),this.width=r,this.height=i}get matrix(){return this.uniforms.matrix}set matrix(e){e.forEach((r,i)=>{this.uniforms.matrix[i]=r})}get width(){return 1/this.uniforms.texelSize[0]}set width(e){this.uniforms.texelSize[0]=1/e}get height(){return 1/this.uniforms.texelSize[1]}set height(e){this.uniforms.texelSize[1]=1/e}}var we=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Me=`precision mediump float;

varying vec2 vTextureCoord;

uniform sampler2D uSampler;

void main(void)
{
    float lum = length(texture2D(uSampler, vTextureCoord.xy).rgb);

    gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);

    if (lum < 1.00)
    {
        if (mod(gl_FragCoord.x + gl_FragCoord.y, 10.0) == 0.0)
        {
            gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    }

    if (lum < 0.75)
    {
        if (mod(gl_FragCoord.x - gl_FragCoord.y, 10.0) == 0.0)
        {
            gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    }

    if (lum < 0.50)
    {
        if (mod(gl_FragCoord.x + gl_FragCoord.y - 5.0, 10.0) == 0.0)
        {
            gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    }

    if (lum < 0.3)
    {
        if (mod(gl_FragCoord.x - gl_FragCoord.y - 5.0, 10.0) == 0.0)
        {
            gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    }
}
`;class De extends o.Filter{constructor(){super(we,Me)}}var Oe=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Re=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;

uniform vec4 filterArea;
uniform vec2 dimensions;

const float SQRT_2 = 1.414213;

const float light = 1.0;

uniform float curvature;
uniform float lineWidth;
uniform float lineContrast;
uniform bool verticalLine;
uniform float noise;
uniform float noiseSize;

uniform float vignetting;
uniform float vignettingAlpha;
uniform float vignettingBlur;

uniform float seed;
uniform float time;

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

void main(void)
{
    vec2 pixelCoord = vTextureCoord.xy * filterArea.xy;
    vec2 dir = vec2(vTextureCoord.xy * filterArea.xy / dimensions - vec2(0.5, 0.5));
    
    gl_FragColor = texture2D(uSampler, vTextureCoord);
    vec3 rgb = gl_FragColor.rgb;

    if (noise > 0.0 && noiseSize > 0.0)
    {
        pixelCoord.x = floor(pixelCoord.x / noiseSize);
        pixelCoord.y = floor(pixelCoord.y / noiseSize);
        float _noise = rand(pixelCoord * noiseSize * seed) - 0.5;
        rgb += _noise * noise;
    }

    if (lineWidth > 0.0)
    {
        float _c = curvature > 0. ? curvature : 1.;
        float k = curvature > 0. ?(length(dir * dir) * 0.25 * _c * _c + 0.935 * _c) : 1.;
        vec2 uv = dir * k;

        float v = (verticalLine ? uv.x * dimensions.x : uv.y * dimensions.y) * min(1.0, 2.0 / lineWidth ) / _c;
        float j = 1. + cos(v * 1.2 - time) * 0.5 * lineContrast;
        rgb *= j;
        float segment = verticalLine ? mod((dir.x + .5) * dimensions.x, 4.) : mod((dir.y + .5) * dimensions.y, 4.);
        rgb *= 0.99 + ceil(segment) * 0.015;
    }

    if (vignetting > 0.0)
    {
        float outter = SQRT_2 - vignetting * SQRT_2;
        float darker = clamp((outter - length(dir) * SQRT_2) / ( 0.00001 + vignettingBlur * SQRT_2), 0.0, 1.0);
        rgb *= darker + (1.0 - darker) * (1.0 - vignettingAlpha);
    }

    gl_FragColor.rgb = rgb;
}
`;const P=class extends o.Filter{constructor(t){super(Oe,Re),this.time=0,this.seed=0,this.uniforms.dimensions=new Float32Array(2),Object.assign(this,P.defaults,t)}apply(t,e,r,i){const{width:s,height:a}=e.filterFrame;this.uniforms.dimensions[0]=s,this.uniforms.dimensions[1]=a,this.uniforms.seed=this.seed,this.uniforms.time=this.time,t.applyFilter(this,e,r,i)}set curvature(t){this.uniforms.curvature=t}get curvature(){return this.uniforms.curvature}set lineWidth(t){this.uniforms.lineWidth=t}get lineWidth(){return this.uniforms.lineWidth}set lineContrast(t){this.uniforms.lineContrast=t}get lineContrast(){return this.uniforms.lineContrast}set verticalLine(t){this.uniforms.verticalLine=t}get verticalLine(){return this.uniforms.verticalLine}set noise(t){this.uniforms.noise=t}get noise(){return this.uniforms.noise}set noiseSize(t){this.uniforms.noiseSize=t}get noiseSize(){return this.uniforms.noiseSize}set vignetting(t){this.uniforms.vignetting=t}get vignetting(){return this.uniforms.vignetting}set vignettingAlpha(t){this.uniforms.vignettingAlpha=t}get vignettingAlpha(){return this.uniforms.vignettingAlpha}set vignettingBlur(t){this.uniforms.vignettingBlur=t}get vignettingBlur(){return this.uniforms.vignettingBlur}};let w=P;w.defaults={curvature:1,lineWidth:1,lineContrast:.25,verticalLine:!1,noise:0,noiseSize:1,seed:0,vignetting:.3,vignettingAlpha:1,vignettingBlur:.3,time:0};var $e=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Ee=`precision mediump float;

varying vec2 vTextureCoord;
varying vec4 vColor;

uniform vec4 filterArea;
uniform sampler2D uSampler;

uniform float angle;
uniform float scale;

float pattern()
{
   float s = sin(angle), c = cos(angle);
   vec2 tex = vTextureCoord * filterArea.xy;
   vec2 point = vec2(
       c * tex.x - s * tex.y,
       s * tex.x + c * tex.y
   ) * scale;
   return (sin(point.x) * sin(point.y)) * 4.0;
}

void main()
{
   vec4 color = texture2D(uSampler, vTextureCoord);
   float average = (color.r + color.g + color.b) / 3.0;
   gl_FragColor = vec4(vec3(average * 10.0 - 5.0 + pattern()), color.a);
}
`;class ke extends o.Filter{constructor(e=1,r=5){super($e,Ee),this.scale=e,this.angle=r}get scale(){return this.uniforms.scale}set scale(e){this.uniforms.scale=e}get angle(){return this.uniforms.angle}set angle(e){this.uniforms.angle=e}}var je=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Le=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;
uniform float alpha;
uniform vec3 color;

uniform vec2 shift;
uniform vec4 inputSize;

void main(void){
    vec4 sample = texture2D(uSampler, vTextureCoord - shift * inputSize.zw);

    // Premultiply alpha
    sample.rgb = color.rgb * sample.a;

    // alpha user alpha
    sample *= alpha;

    gl_FragColor = sample;
}`,Ie=Object.defineProperty,M=Object.getOwnPropertySymbols,Ve=Object.prototype.hasOwnProperty,Ne=Object.prototype.propertyIsEnumerable,D=(t,e,r)=>e in t?Ie(t,e,{enumerable:!0,configurable:!0,writable:!0,value:r}):t[e]=r,O=(t,e)=>{for(var r in e||(e={}))Ve.call(e,r)&&D(t,r,e[r]);if(M)for(var r of M(e))Ne.call(e,r)&&D(t,r,e[r]);return t};const x=class extends o.Filter{constructor(t){super(),this.angle=45,this._distance=5,this._resolution=o.settings.FILTER_RESOLUTION;const e=t?O(O({},x.defaults),t):x.defaults,{kernels:r,blur:i,quality:s,pixelSize:a,resolution:l}=e;this._tintFilter=new o.Filter(je,Le),this._tintFilter.uniforms.color=new Float32Array(4),this._tintFilter.uniforms.shift=new o.Point,this._tintFilter.resolution=l,this._blurFilter=r?new h(r):new h(i,s),this.pixelSize=a,this.resolution=l;const{shadowOnly:u,rotation:c,distance:d,alpha:m,color:g}=e;this.shadowOnly=u,this.rotation=c,this.distance=d,this.alpha=m,this.color=g,this._updatePadding()}apply(t,e,r,i){const s=t.getFilterTexture();this._tintFilter.apply(t,e,s,1),this._blurFilter.apply(t,s,r,i),this.shadowOnly!==!0&&t.applyFilter(this,e,r,0),t.returnFilterTexture(s)}_updatePadding(){this.padding=this.distance+this.blur*2}_updateShift(){this._tintFilter.uniforms.shift.set(this.distance*Math.cos(this.angle),this.distance*Math.sin(this.angle))}get resolution(){return this._resolution}set resolution(t){this._resolution=t,this._tintFilter&&(this._tintFilter.resolution=t),this._blurFilter&&(this._blurFilter.resolution=t)}get distance(){return this._distance}set distance(t){this._distance=t,this._updatePadding(),this._updateShift()}get rotation(){return this.angle/o.DEG_TO_RAD}set rotation(t){this.angle=t*o.DEG_TO_RAD,this._updateShift()}get alpha(){return this._tintFilter.uniforms.alpha}set alpha(t){this._tintFilter.uniforms.alpha=t}get color(){return o.utils.rgb2hex(this._tintFilter.uniforms.color)}set color(t){o.utils.hex2rgb(t,this._tintFilter.uniforms.color)}get kernels(){return this._blurFilter.kernels}set kernels(t){this._blurFilter.kernels=t}get blur(){return this._blurFilter.blur}set blur(t){this._blurFilter.blur=t,this._updatePadding()}get quality(){return this._blurFilter.quality}set quality(t){this._blurFilter.quality=t}get pixelSize(){return this._blurFilter.pixelSize}set pixelSize(t){this._blurFilter.pixelSize=t}};let R=x;R.defaults={rotation:45,distance:5,color:0,alpha:.5,shadowOnly:!1,kernels:null,blur:2,quality:3,pixelSize:1,resolution:o.settings.FILTER_RESOLUTION};var Xe=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Be=`precision mediump float;

varying vec2 vTextureCoord;

uniform sampler2D uSampler;
uniform float strength;
uniform vec4 filterArea;


void main(void)
{
	vec2 onePixel = vec2(1.0 / filterArea);

	vec4 color;

	color.rgb = vec3(0.5);

	color -= texture2D(uSampler, vTextureCoord - onePixel) * strength;
	color += texture2D(uSampler, vTextureCoord + onePixel) * strength;

	color.rgb = vec3((color.r + color.g + color.b) / 3.0);

	float alpha = texture2D(uSampler, vTextureCoord).a;

	gl_FragColor = vec4(color.rgb * alpha, alpha);
}
`;class Ge extends o.Filter{constructor(e=5){super(Xe,Be),this.strength=e}get strength(){return this.uniforms.strength}set strength(e){this.uniforms.strength=e}}var qe=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Ke=`// precision highp float;

varying vec2 vTextureCoord;
uniform sampler2D uSampler;

uniform vec4 filterArea;
uniform vec4 filterClamp;
uniform vec2 dimensions;
uniform float aspect;

uniform sampler2D displacementMap;
uniform float offset;
uniform float sinDir;
uniform float cosDir;
uniform int fillMode;

uniform float seed;
uniform vec2 red;
uniform vec2 green;
uniform vec2 blue;

const int TRANSPARENT = 0;
const int ORIGINAL = 1;
const int LOOP = 2;
const int CLAMP = 3;
const int MIRROR = 4;

void main(void)
{
    vec2 coord = (vTextureCoord * filterArea.xy) / dimensions;

    if (coord.x > 1.0 || coord.y > 1.0) {
        return;
    }

    float cx = coord.x - 0.5;
    float cy = (coord.y - 0.5) * aspect;
    float ny = (-sinDir * cx + cosDir * cy) / aspect + 0.5;

    // displacementMap: repeat
    // ny = ny > 1.0 ? ny - 1.0 : (ny < 0.0 ? 1.0 + ny : ny);

    // displacementMap: mirror
    ny = ny > 1.0 ? 2.0 - ny : (ny < 0.0 ? -ny : ny);

    vec4 dc = texture2D(displacementMap, vec2(0.5, ny));

    float displacement = (dc.r - dc.g) * (offset / filterArea.x);

    coord = vTextureCoord + vec2(cosDir * displacement, sinDir * displacement * aspect);

    if (fillMode == CLAMP) {
        coord = clamp(coord, filterClamp.xy, filterClamp.zw);
    } else {
        if( coord.x > filterClamp.z ) {
            if (fillMode == TRANSPARENT) {
                discard;
            } else if (fillMode == LOOP) {
                coord.x -= filterClamp.z;
            } else if (fillMode == MIRROR) {
                coord.x = filterClamp.z * 2.0 - coord.x;
            }
        } else if( coord.x < filterClamp.x ) {
            if (fillMode == TRANSPARENT) {
                discard;
            } else if (fillMode == LOOP) {
                coord.x += filterClamp.z;
            } else if (fillMode == MIRROR) {
                coord.x *= -filterClamp.z;
            }
        }

        if( coord.y > filterClamp.w ) {
            if (fillMode == TRANSPARENT) {
                discard;
            } else if (fillMode == LOOP) {
                coord.y -= filterClamp.w;
            } else if (fillMode == MIRROR) {
                coord.y = filterClamp.w * 2.0 - coord.y;
            }
        } else if( coord.y < filterClamp.y ) {
            if (fillMode == TRANSPARENT) {
                discard;
            } else if (fillMode == LOOP) {
                coord.y += filterClamp.w;
            } else if (fillMode == MIRROR) {
                coord.y *= -filterClamp.w;
            }
        }
    }

    gl_FragColor.r = texture2D(uSampler, coord + red * (1.0 - seed * 0.4) / filterArea.xy).r;
    gl_FragColor.g = texture2D(uSampler, coord + green * (1.0 - seed * 0.3) / filterArea.xy).g;
    gl_FragColor.b = texture2D(uSampler, coord + blue * (1.0 - seed * 0.2) / filterArea.xy).b;
    gl_FragColor.a = texture2D(uSampler, coord).a;
}
`;const p=class extends o.Filter{constructor(t){super(qe,Ke),this.offset=100,this.fillMode=p.TRANSPARENT,this.average=!1,this.seed=0,this.minSize=8,this.sampleSize=512,this._slices=0,this._offsets=new Float32Array(1),this._sizes=new Float32Array(1),this._direction=-1,this.uniforms.dimensions=new Float32Array(2),this._canvas=document.createElement("canvas"),this._canvas.width=4,this._canvas.height=this.sampleSize,this.texture=o.Texture.from(this._canvas,{scaleMode:o.SCALE_MODES.NEAREST}),Object.assign(this,p.defaults,t)}apply(t,e,r,i){const{width:s,height:a}=e.filterFrame;this.uniforms.dimensions[0]=s,this.uniforms.dimensions[1]=a,this.uniforms.aspect=a/s,this.uniforms.seed=this.seed,this.uniforms.offset=this.offset,this.uniforms.fillMode=this.fillMode,t.applyFilter(this,e,r,i)}_randomizeSizes(){const t=this._sizes,e=this._slices-1,r=this.sampleSize,i=Math.min(this.minSize/r,.9/this._slices);if(this.average){const s=this._slices;let a=1;for(let l=0;l<e;l++){const u=a/(s-l),c=Math.max(u*(1-Math.random()*.6),i);t[l]=c,a-=c}t[e]=a}else{let s=1;const a=Math.sqrt(1/this._slices);for(let l=0;l<e;l++){const u=Math.max(a*s*Math.random(),i);t[l]=u,s-=u}t[e]=s}this.shuffle()}shuffle(){const t=this._sizes,e=this._slices-1;for(let r=e;r>0;r--){const i=Math.random()*r>>0,s=t[r];t[r]=t[i],t[i]=s}}_randomizeOffsets(){for(let t=0;t<this._slices;t++)this._offsets[t]=Math.random()*(Math.random()<.5?-1:1)}refresh(){this._randomizeSizes(),this._randomizeOffsets(),this.redraw()}redraw(){const t=this.sampleSize,e=this.texture,r=this._canvas.getContext("2d");r.clearRect(0,0,8,t);let i,s=0;for(let a=0;a<this._slices;a++){i=Math.floor(this._offsets[a]*256);const l=this._sizes[a]*t,u=i>0?i:0,c=i<0?-i:0;r.fillStyle=`rgba(${u}, ${c}, 0, 1)`,r.fillRect(0,s>>0,t,l+1>>0),s+=l}e.baseTexture.update(),this.uniforms.displacementMap=e}set sizes(t){const e=Math.min(this._slices,t.length);for(let r=0;r<e;r++)this._sizes[r]=t[r]}get sizes(){return this._sizes}set offsets(t){const e=Math.min(this._slices,t.length);for(let r=0;r<e;r++)this._offsets[r]=t[r]}get offsets(){return this._offsets}get slices(){return this._slices}set slices(t){this._slices!==t&&(this._slices=t,this.uniforms.slices=t,this._sizes=this.uniforms.slicesWidth=new Float32Array(t),this._offsets=this.uniforms.slicesOffset=new Float32Array(t),this.refresh())}get direction(){return this._direction}set direction(t){if(this._direction===t)return;this._direction=t;const e=t*o.DEG_TO_RAD;this.uniforms.sinDir=Math.sin(e),this.uniforms.cosDir=Math.cos(e)}get red(){return this.uniforms.red}set red(t){this.uniforms.red=t}get green(){return this.uniforms.green}set green(t){this.uniforms.green=t}get blue(){return this.uniforms.blue}set blue(t){this.uniforms.blue=t}destroy(){var t;(t=this.texture)==null||t.destroy(!0),this.texture=this._canvas=this.red=this.green=this.blue=this._sizes=this._offsets=null}};let f=p;f.defaults={slices:5,offset:100,direction:0,fillMode:0,average:!1,seed:0,red:[0,0],green:[0,0],blue:[0,0],minSize:8,sampleSize:512},f.TRANSPARENT=0,f.ORIGINAL=1,f.LOOP=2,f.CLAMP=3,f.MIRROR=4;var We=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Ye=`varying vec2 vTextureCoord;
varying vec4 vColor;

uniform sampler2D uSampler;

uniform float outerStrength;
uniform float innerStrength;

uniform vec4 glowColor;

uniform vec4 filterArea;
uniform vec4 filterClamp;
uniform bool knockout;

const float PI = 3.14159265358979323846264;

const float DIST = __DIST__;
const float ANGLE_STEP_SIZE = min(__ANGLE_STEP_SIZE__, PI * 2.0);
const float ANGLE_STEP_NUM = ceil(PI * 2.0 / ANGLE_STEP_SIZE);

const float MAX_TOTAL_ALPHA = ANGLE_STEP_NUM * DIST * (DIST + 1.0) / 2.0;

void main(void) {
    vec2 px = vec2(1.0 / filterArea.x, 1.0 / filterArea.y);

    float totalAlpha = 0.0;

    vec2 direction;
    vec2 displaced;
    vec4 curColor;

    for (float angle = 0.0; angle < PI * 2.0; angle += ANGLE_STEP_SIZE) {
       direction = vec2(cos(angle), sin(angle)) * px;

       for (float curDistance = 0.0; curDistance < DIST; curDistance++) {
           displaced = clamp(vTextureCoord + direction * 
                   (curDistance + 1.0), filterClamp.xy, filterClamp.zw);

           curColor = texture2D(uSampler, displaced);

           totalAlpha += (DIST - curDistance) * curColor.a;
       }
    }
    
    curColor = texture2D(uSampler, vTextureCoord);

    float alphaRatio = (totalAlpha / MAX_TOTAL_ALPHA);

    float innerGlowAlpha = (1.0 - alphaRatio) * innerStrength * curColor.a;
    float innerGlowStrength = min(1.0, innerGlowAlpha);
    
    vec4 innerColor = mix(curColor, glowColor, innerGlowStrength);

    float outerGlowAlpha = alphaRatio * outerStrength * (1. - curColor.a);
    float outerGlowStrength = min(1.0 - innerColor.a, outerGlowAlpha);

    vec4 outerGlowColor = outerGlowStrength * glowColor.rgba;
    
    if (knockout) {
      float resultAlpha = outerGlowAlpha + innerGlowAlpha;
      gl_FragColor = vec4(glowColor.rgb * resultAlpha, resultAlpha);
    }
    else {
      gl_FragColor = innerColor + outerGlowColor;
    }
}
`;const $=class extends o.Filter{constructor(t){const e=Object.assign({},$.defaults,t),{outerStrength:r,innerStrength:i,color:s,knockout:a,quality:l}=e,u=Math.round(e.distance);super(We,Ye.replace(/__ANGLE_STEP_SIZE__/gi,`${(1/l/u).toFixed(7)}`).replace(/__DIST__/gi,`${u.toFixed(0)}.0`)),this.uniforms.glowColor=new Float32Array([0,0,0,1]),Object.assign(this,{color:s,outerStrength:r,innerStrength:i,padding:u,knockout:a})}get color(){return o.utils.rgb2hex(this.uniforms.glowColor)}set color(t){o.utils.hex2rgb(t,this.uniforms.glowColor)}get outerStrength(){return this.uniforms.outerStrength}set outerStrength(t){this.uniforms.outerStrength=t}get innerStrength(){return this.uniforms.innerStrength}set innerStrength(t){this.uniforms.innerStrength=t}get knockout(){return this.uniforms.knockout}set knockout(t){this.uniforms.knockout=t}};let E=$;E.defaults={distance:10,outerStrength:4,innerStrength:0,color:16777215,quality:.1,knockout:!1};var Ze=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Ue=`vec3 mod289(vec3 x)
{
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}
vec4 mod289(vec4 x)
{
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}
vec4 permute(vec4 x)
{
    return mod289(((x * 34.0) + 1.0) * x);
}
vec4 taylorInvSqrt(vec4 r)
{
    return 1.79284291400159 - 0.85373472095314 * r;
}
vec3 fade(vec3 t)
{
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
}
// Classic Perlin noise, periodic variant
float pnoise(vec3 P, vec3 rep)
{
    vec3 Pi0 = mod(floor(P), rep); // Integer part, modulo period
    vec3 Pi1 = mod(Pi0 + vec3(1.0), rep); // Integer part + 1, mod period
    Pi0 = mod289(Pi0);
    Pi1 = mod289(Pi1);
    vec3 Pf0 = fract(P); // Fractional part for interpolation
    vec3 Pf1 = Pf0 - vec3(1.0); // Fractional part - 1.0
    vec4 ix = vec4(Pi0.x, Pi1.x, Pi0.x, Pi1.x);
    vec4 iy = vec4(Pi0.yy, Pi1.yy);
    vec4 iz0 = Pi0.zzzz;
    vec4 iz1 = Pi1.zzzz;
    vec4 ixy = permute(permute(ix) + iy);
    vec4 ixy0 = permute(ixy + iz0);
    vec4 ixy1 = permute(ixy + iz1);
    vec4 gx0 = ixy0 * (1.0 / 7.0);
    vec4 gy0 = fract(floor(gx0) * (1.0 / 7.0)) - 0.5;
    gx0 = fract(gx0);
    vec4 gz0 = vec4(0.5) - abs(gx0) - abs(gy0);
    vec4 sz0 = step(gz0, vec4(0.0));
    gx0 -= sz0 * (step(0.0, gx0) - 0.5);
    gy0 -= sz0 * (step(0.0, gy0) - 0.5);
    vec4 gx1 = ixy1 * (1.0 / 7.0);
    vec4 gy1 = fract(floor(gx1) * (1.0 / 7.0)) - 0.5;
    gx1 = fract(gx1);
    vec4 gz1 = vec4(0.5) - abs(gx1) - abs(gy1);
    vec4 sz1 = step(gz1, vec4(0.0));
    gx1 -= sz1 * (step(0.0, gx1) - 0.5);
    gy1 -= sz1 * (step(0.0, gy1) - 0.5);
    vec3 g000 = vec3(gx0.x, gy0.x, gz0.x);
    vec3 g100 = vec3(gx0.y, gy0.y, gz0.y);
    vec3 g010 = vec3(gx0.z, gy0.z, gz0.z);
    vec3 g110 = vec3(gx0.w, gy0.w, gz0.w);
    vec3 g001 = vec3(gx1.x, gy1.x, gz1.x);
    vec3 g101 = vec3(gx1.y, gy1.y, gz1.y);
    vec3 g011 = vec3(gx1.z, gy1.z, gz1.z);
    vec3 g111 = vec3(gx1.w, gy1.w, gz1.w);
    vec4 norm0 = taylorInvSqrt(vec4(dot(g000, g000), dot(g010, g010), dot(g100, g100), dot(g110, g110)));
    g000 *= norm0.x;
    g010 *= norm0.y;
    g100 *= norm0.z;
    g110 *= norm0.w;
    vec4 norm1 = taylorInvSqrt(vec4(dot(g001, g001), dot(g011, g011), dot(g101, g101), dot(g111, g111)));
    g001 *= norm1.x;
    g011 *= norm1.y;
    g101 *= norm1.z;
    g111 *= norm1.w;
    float n000 = dot(g000, Pf0);
    float n100 = dot(g100, vec3(Pf1.x, Pf0.yz));
    float n010 = dot(g010, vec3(Pf0.x, Pf1.y, Pf0.z));
    float n110 = dot(g110, vec3(Pf1.xy, Pf0.z));
    float n001 = dot(g001, vec3(Pf0.xy, Pf1.z));
    float n101 = dot(g101, vec3(Pf1.x, Pf0.y, Pf1.z));
    float n011 = dot(g011, vec3(Pf0.x, Pf1.yz));
    float n111 = dot(g111, Pf1);
    vec3 fade_xyz = fade(Pf0);
    vec4 n_z = mix(vec4(n000, n100, n010, n110), vec4(n001, n101, n011, n111), fade_xyz.z);
    vec2 n_yz = mix(n_z.xy, n_z.zw, fade_xyz.y);
    float n_xyz = mix(n_yz.x, n_yz.y, fade_xyz.x);
    return 2.2 * n_xyz;
}
float turb(vec3 P, vec3 rep, float lacunarity, float gain)
{
    float sum = 0.0;
    float sc = 1.0;
    float totalgain = 1.0;
    for (float i = 0.0; i < 6.0; i++)
    {
        sum += totalgain * pnoise(P * sc, rep);
        sc *= lacunarity;
        totalgain *= gain;
    }
    return abs(sum);
}
`,Qe=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;
uniform vec4 filterArea;
uniform vec2 dimensions;

uniform vec2 light;
uniform bool parallel;
uniform float aspect;

uniform float gain;
uniform float lacunarity;
uniform float time;
uniform float alpha;

\${perlin}

void main(void) {
    vec2 coord = vTextureCoord * filterArea.xy / dimensions.xy;

    float d;

    if (parallel) {
        float _cos = light.x;
        float _sin = light.y;
        d = (_cos * coord.x) + (_sin * coord.y * aspect);
    } else {
        float dx = coord.x - light.x / dimensions.x;
        float dy = (coord.y - light.y / dimensions.y) * aspect;
        float dis = sqrt(dx * dx + dy * dy) + 0.00001;
        d = dy / dis;
    }

    vec3 dir = vec3(d, d, 0.0);

    float noise = turb(dir + vec3(time, 0.0, 62.1 + time) * 0.05, vec3(480.0, 320.0, 480.0), lacunarity, gain);
    noise = mix(noise, 0.0, 0.3);
    //fade vertically.
    vec4 mist = vec4(noise, noise, noise, 1.0) * (1.0 - coord.y);
    mist.a = 1.0;
    // apply user alpha
    mist *= alpha;

    gl_FragColor = texture2D(uSampler, vTextureCoord) + mist;

}
`;const k=class extends o.Filter{constructor(t){super(Ze,Qe.replace("${perlin}",Ue)),this.parallel=!0,this.time=0,this._angle=0,this.uniforms.dimensions=new Float32Array(2);const e=Object.assign(k.defaults,t);this._angleLight=new o.Point,this.angle=e.angle,this.gain=e.gain,this.lacunarity=e.lacunarity,this.alpha=e.alpha,this.parallel=e.parallel,this.center=e.center,this.time=e.time}apply(t,e,r,i){const{width:s,height:a}=e.filterFrame;this.uniforms.light=this.parallel?this._angleLight:this.center,this.uniforms.parallel=this.parallel,this.uniforms.dimensions[0]=s,this.uniforms.dimensions[1]=a,this.uniforms.aspect=a/s,this.uniforms.time=this.time,this.uniforms.alpha=this.alpha,t.applyFilter(this,e,r,i)}get angle(){return this._angle}set angle(t){this._angle=t;const e=t*o.DEG_TO_RAD;this._angleLight.x=Math.cos(e),this._angleLight.y=Math.sin(e)}get gain(){return this.uniforms.gain}set gain(t){this.uniforms.gain=t}get lacunarity(){return this.uniforms.lacunarity}set lacunarity(t){this.uniforms.lacunarity=t}get alpha(){return this.uniforms.alpha}set alpha(t){this.uniforms.alpha=t}};let j=k;j.defaults={angle:30,gain:.5,lacunarity:2.5,time:0,parallel:!0,center:[0,0],alpha:1};var He=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Je=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;
uniform vec4 filterArea;

uniform vec2 uVelocity;
uniform int uKernelSize;
uniform float uOffset;

const int MAX_KERNEL_SIZE = 2048;

// Notice:
// the perfect way:
//    int kernelSize = min(uKernelSize, MAX_KERNELSIZE);
// BUT in real use-case , uKernelSize < MAX_KERNELSIZE almost always.
// So use uKernelSize directly.

void main(void)
{
    vec4 color = texture2D(uSampler, vTextureCoord);

    if (uKernelSize == 0)
    {
        gl_FragColor = color;
        return;
    }

    vec2 velocity = uVelocity / filterArea.xy;
    float offset = -uOffset / length(uVelocity) - 0.5;
    int k = uKernelSize - 1;

    for(int i = 0; i < MAX_KERNEL_SIZE - 1; i++) {
        if (i == k) {
            break;
        }
        vec2 bias = velocity * (float(i) / float(k) + offset);
        color += texture2D(uSampler, vTextureCoord + bias);
    }
    gl_FragColor = color / float(uKernelSize);
}
`;class et extends o.Filter{constructor(e=[0,0],r=5,i=0){super(He,Je),this.kernelSize=5,this.uniforms.uVelocity=new Float32Array(2),this._velocity=new o.ObservablePoint(this.velocityChanged,this),this.setVelocity(e),this.kernelSize=r,this.offset=i}apply(e,r,i,s){const{x:a,y:l}=this.velocity;this.uniforms.uKernelSize=a!==0||l!==0?this.kernelSize:0,e.applyFilter(this,r,i,s)}set velocity(e){this.setVelocity(e)}get velocity(){return this._velocity}setVelocity(e){if(Array.isArray(e)){const[r,i]=e;this._velocity.set(r,i)}else this._velocity.copyFrom(e)}velocityChanged(){this.uniforms.uVelocity[0]=this._velocity.x,this.uniforms.uVelocity[1]=this._velocity.y,this.padding=(Math.max(Math.abs(this._velocity.x),Math.abs(this._velocity.y))>>0)+1}set offset(e){this.uniforms.uOffset=e}get offset(){return this.uniforms.uOffset}}var tt=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,rt=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;

uniform float epsilon;

const int MAX_COLORS = %maxColors%;

uniform vec3 originalColors[MAX_COLORS];
uniform vec3 targetColors[MAX_COLORS];

void main(void)
{
    gl_FragColor = texture2D(uSampler, vTextureCoord);

    float alpha = gl_FragColor.a;
    if (alpha < 0.0001)
    {
      return;
    }

    vec3 color = gl_FragColor.rgb / alpha;

    for(int i = 0; i < MAX_COLORS; i++)
    {
      vec3 origColor = originalColors[i];
      if (origColor.r < 0.0)
      {
        break;
      }
      vec3 colorDiff = origColor - color;
      if (length(colorDiff) < epsilon)
      {
        vec3 targetColor = targetColors[i];
        gl_FragColor = vec4((targetColor + colorDiff) * alpha, alpha);
        return;
      }
    }
}
`;class it extends o.Filter{constructor(e,r=.05,i=e.length){super(tt,rt.replace(/%maxColors%/g,i.toFixed(0))),this._replacements=[],this._maxColors=0,this.epsilon=r,this._maxColors=i,this.uniforms.originalColors=new Float32Array(i*3),this.uniforms.targetColors=new Float32Array(i*3),this.replacements=e}set replacements(e){const r=this.uniforms.originalColors,i=this.uniforms.targetColors,s=e.length;if(s>this._maxColors)throw new Error(`Length of replacements (${s}) exceeds the maximum colors length (${this._maxColors})`);r[s*3]=-1;for(let a=0;a<s;a++){const l=e[a];let u=l[0];typeof u=="number"?u=o.utils.hex2rgb(u):l[0]=o.utils.rgb2hex(u),r[a*3]=u[0],r[a*3+1]=u[1],r[a*3+2]=u[2];let c=l[1];typeof c=="number"?c=o.utils.hex2rgb(c):l[1]=o.utils.rgb2hex(c),i[a*3]=c[0],i[a*3+1]=c[1],i[a*3+2]=c[2]}this._replacements=e}get replacements(){return this._replacements}refresh(){this.replacements=this._replacements}get maxColors(){return this._maxColors}set epsilon(e){this.uniforms.epsilon=e}get epsilon(){return this.uniforms.epsilon}}var ot=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,st=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;
uniform vec4 filterArea;
uniform vec2 dimensions;

uniform float sepia;
uniform float noise;
uniform float noiseSize;
uniform float scratch;
uniform float scratchDensity;
uniform float scratchWidth;
uniform float vignetting;
uniform float vignettingAlpha;
uniform float vignettingBlur;
uniform float seed;

const float SQRT_2 = 1.414213;
const vec3 SEPIA_RGB = vec3(112.0 / 255.0, 66.0 / 255.0, 20.0 / 255.0);

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

vec3 Overlay(vec3 src, vec3 dst)
{
    // if (dst <= 0.5) then: 2 * src * dst
    // if (dst > 0.5) then: 1 - 2 * (1 - dst) * (1 - src)
    return vec3((dst.x <= 0.5) ? (2.0 * src.x * dst.x) : (1.0 - 2.0 * (1.0 - dst.x) * (1.0 - src.x)),
                (dst.y <= 0.5) ? (2.0 * src.y * dst.y) : (1.0 - 2.0 * (1.0 - dst.y) * (1.0 - src.y)),
                (dst.z <= 0.5) ? (2.0 * src.z * dst.z) : (1.0 - 2.0 * (1.0 - dst.z) * (1.0 - src.z)));
}


void main()
{
    gl_FragColor = texture2D(uSampler, vTextureCoord);
    vec3 color = gl_FragColor.rgb;

    if (sepia > 0.0)
    {
        float gray = (color.x + color.y + color.z) / 3.0;
        vec3 grayscale = vec3(gray);

        color = Overlay(SEPIA_RGB, grayscale);

        color = grayscale + sepia * (color - grayscale);
    }

    vec2 coord = vTextureCoord * filterArea.xy / dimensions.xy;

    if (vignetting > 0.0)
    {
        float outter = SQRT_2 - vignetting * SQRT_2;
        vec2 dir = vec2(vec2(0.5, 0.5) - coord);
        dir.y *= dimensions.y / dimensions.x;
        float darker = clamp((outter - length(dir) * SQRT_2) / ( 0.00001 + vignettingBlur * SQRT_2), 0.0, 1.0);
        color.rgb *= darker + (1.0 - darker) * (1.0 - vignettingAlpha);
    }

    if (scratchDensity > seed && scratch != 0.0)
    {
        float phase = seed * 256.0;
        float s = mod(floor(phase), 2.0);
        float dist = 1.0 / scratchDensity;
        float d = distance(coord, vec2(seed * dist, abs(s - seed * dist)));
        if (d < seed * 0.6 + 0.4)
        {
            highp float period = scratchDensity * 10.0;

            float xx = coord.x * period + phase;
            float aa = abs(mod(xx, 0.5) * 4.0);
            float bb = mod(floor(xx / 0.5), 2.0);
            float yy = (1.0 - bb) * aa + bb * (2.0 - aa);

            float kk = 2.0 * period;
            float dw = scratchWidth / dimensions.x * (0.75 + seed);
            float dh = dw * kk;

            float tine = (yy - (2.0 - dh));

            if (tine > 0.0) {
                float _sign = sign(scratch);

                tine = s * tine / period + scratch + 0.1;
                tine = clamp(tine + 1.0, 0.5 + _sign * 0.5, 1.5 + _sign * 0.5);

                color.rgb *= tine;
            }
        }
    }

    if (noise > 0.0 && noiseSize > 0.0)
    {
        vec2 pixelCoord = vTextureCoord.xy * filterArea.xy;
        pixelCoord.x = floor(pixelCoord.x / noiseSize);
        pixelCoord.y = floor(pixelCoord.y / noiseSize);
        // vec2 d = pixelCoord * noiseSize * vec2(1024.0 + seed * 512.0, 1024.0 - seed * 512.0);
        // float _noise = snoise(d) * 0.5;
        float _noise = rand(pixelCoord * noiseSize * seed) - 0.5;
        color += _noise * noise;
    }

    gl_FragColor.rgb = color;
}
`;const L=class extends o.Filter{constructor(t,e=0){super(ot,st),this.seed=0,this.uniforms.dimensions=new Float32Array(2),typeof t=="number"?(this.seed=t,t=void 0):this.seed=e,Object.assign(this,L.defaults,t)}apply(t,e,r,i){var s,a;this.uniforms.dimensions[0]=(s=e.filterFrame)==null?void 0:s.width,this.uniforms.dimensions[1]=(a=e.filterFrame)==null?void 0:a.height,this.uniforms.seed=this.seed,t.applyFilter(this,e,r,i)}set sepia(t){this.uniforms.sepia=t}get sepia(){return this.uniforms.sepia}set noise(t){this.uniforms.noise=t}get noise(){return this.uniforms.noise}set noiseSize(t){this.uniforms.noiseSize=t}get noiseSize(){return this.uniforms.noiseSize}set scratch(t){this.uniforms.scratch=t}get scratch(){return this.uniforms.scratch}set scratchDensity(t){this.uniforms.scratchDensity=t}get scratchDensity(){return this.uniforms.scratchDensity}set scratchWidth(t){this.uniforms.scratchWidth=t}get scratchWidth(){return this.uniforms.scratchWidth}set vignetting(t){this.uniforms.vignetting=t}get vignetting(){return this.uniforms.vignetting}set vignettingAlpha(t){this.uniforms.vignettingAlpha=t}get vignettingAlpha(){return this.uniforms.vignettingAlpha}set vignettingBlur(t){this.uniforms.vignettingBlur=t}get vignettingBlur(){return this.uniforms.vignettingBlur}};let I=L;I.defaults={sepia:.3,noise:.3,noiseSize:1,scratch:.5,scratchDensity:.3,scratchWidth:1,vignetting:.3,vignettingAlpha:1,vignettingBlur:.3};var at=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,lt=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;

uniform vec2 thickness;
uniform vec4 outlineColor;
uniform vec4 filterClamp;

const float DOUBLE_PI = 3.14159265358979323846264 * 2.;

void main(void) {
    vec4 ownColor = texture2D(uSampler, vTextureCoord);
    vec4 curColor;
    float maxAlpha = 0.;
    vec2 displaced;
    for (float angle = 0.; angle <= DOUBLE_PI; angle += \${angleStep}) {
        displaced.x = vTextureCoord.x + thickness.x * cos(angle);
        displaced.y = vTextureCoord.y + thickness.y * sin(angle);
        curColor = texture2D(uSampler, clamp(displaced, filterClamp.xy, filterClamp.zw));
        maxAlpha = max(maxAlpha, curColor.a);
    }
    float resultAlpha = max(maxAlpha, ownColor.a);
    gl_FragColor = vec4((ownColor.rgb + outlineColor.rgb * (1. - ownColor.a)) * resultAlpha, resultAlpha);
}
`;const v=class extends o.Filter{constructor(t=1,e=0,r=.1){super(at,lt.replace(/\$\{angleStep\}/,v.getAngleStep(r))),this._thickness=1,this.uniforms.thickness=new Float32Array([0,0]),this.uniforms.outlineColor=new Float32Array([0,0,0,1]),Object.assign(this,{thickness:t,color:e,quality:r})}static getAngleStep(t){const e=Math.max(t*v.MAX_SAMPLES,v.MIN_SAMPLES);return(Math.PI*2/e).toFixed(7)}apply(t,e,r,i){this.uniforms.thickness[0]=this._thickness/e._frame.width,this.uniforms.thickness[1]=this._thickness/e._frame.height,t.applyFilter(this,e,r,i)}get color(){return o.utils.rgb2hex(this.uniforms.outlineColor)}set color(t){o.utils.hex2rgb(t,this.uniforms.outlineColor)}get thickness(){return this._thickness}set thickness(t){this._thickness=t,this.padding=t}};let y=v;y.MIN_SAMPLES=1,y.MAX_SAMPLES=100;var nt=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,ut=`precision mediump float;

varying vec2 vTextureCoord;

uniform vec2 size;
uniform sampler2D uSampler;

uniform vec4 filterArea;

vec2 mapCoord( vec2 coord )
{
    coord *= filterArea.xy;
    coord += filterArea.zw;

    return coord;
}

vec2 unmapCoord( vec2 coord )
{
    coord -= filterArea.zw;
    coord /= filterArea.xy;

    return coord;
}

vec2 pixelate(vec2 coord, vec2 size)
{
	return floor( coord / size ) * size;
}

void main(void)
{
    vec2 coord = mapCoord(vTextureCoord);

    coord = pixelate(coord, size);

    coord = unmapCoord(coord);

    gl_FragColor = texture2D(uSampler, coord);
}
`;class ct extends o.Filter{constructor(e=10){super(nt,ut),this.size=e}get size(){return this.uniforms.size}set size(e){typeof e=="number"&&(e=[e,e]),this.uniforms.size=e}}var ft=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,dt=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;
uniform vec4 filterArea;

uniform float uRadian;
uniform vec2 uCenter;
uniform float uRadius;
uniform int uKernelSize;

const int MAX_KERNEL_SIZE = 2048;

void main(void)
{
    vec4 color = texture2D(uSampler, vTextureCoord);

    if (uKernelSize == 0)
    {
        gl_FragColor = color;
        return;
    }

    float aspect = filterArea.y / filterArea.x;
    vec2 center = uCenter.xy / filterArea.xy;
    float gradient = uRadius / filterArea.x * 0.3;
    float radius = uRadius / filterArea.x - gradient * 0.5;
    int k = uKernelSize - 1;

    vec2 coord = vTextureCoord;
    vec2 dir = vec2(center - coord);
    float dist = length(vec2(dir.x, dir.y * aspect));

    float radianStep = uRadian;
    if (radius >= 0.0 && dist > radius) {
        float delta = dist - radius;
        float gap = gradient;
        float scale = 1.0 - abs(delta / gap);
        if (scale <= 0.0) {
            gl_FragColor = color;
            return;
        }
        radianStep *= scale;
    }
    radianStep /= float(k);

    float s = sin(radianStep);
    float c = cos(radianStep);
    mat2 rotationMatrix = mat2(vec2(c, -s), vec2(s, c));

    for(int i = 0; i < MAX_KERNEL_SIZE - 1; i++) {
        if (i == k) {
            break;
        }

        coord -= center;
        coord.y *= aspect;
        coord = rotationMatrix * coord;
        coord.y /= aspect;
        coord += center;

        vec4 sample = texture2D(uSampler, coord);

        // switch to pre-multiplied alpha to correctly blur transparent images
        // sample.rgb *= sample.a;

        color += sample;
    }

    gl_FragColor = color / float(uKernelSize);
}
`;class ht extends o.Filter{constructor(e=0,r=[0,0],i=5,s=-1){super(ft,dt),this._angle=0,this.angle=e,this.center=r,this.kernelSize=i,this.radius=s}apply(e,r,i,s){this.uniforms.uKernelSize=this._angle!==0?this.kernelSize:0,e.applyFilter(this,r,i,s)}set angle(e){this._angle=e,this.uniforms.uRadian=e*Math.PI/180}get angle(){return this._angle}get center(){return this.uniforms.uCenter}set center(e){this.uniforms.uCenter=e}get radius(){return this.uniforms.uRadius}set radius(e){(e<0||e===1/0)&&(e=-1),this.uniforms.uRadius=e}}var mt=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,vt=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;

uniform vec4 filterArea;
uniform vec4 filterClamp;
uniform vec2 dimensions;

uniform bool mirror;
uniform float boundary;
uniform vec2 amplitude;
uniform vec2 waveLength;
uniform vec2 alpha;
uniform float time;

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

void main(void)
{
    vec2 pixelCoord = vTextureCoord.xy * filterArea.xy;
    vec2 coord = pixelCoord / dimensions;

    if (coord.y < boundary) {
        gl_FragColor = texture2D(uSampler, vTextureCoord);
        return;
    }

    float k = (coord.y - boundary) / (1. - boundary + 0.0001);
    float areaY = boundary * dimensions.y / filterArea.y;
    float v = areaY + areaY - vTextureCoord.y;
    float y = mirror ? v : vTextureCoord.y;

    float _amplitude = ((amplitude.y - amplitude.x) * k + amplitude.x ) / filterArea.x;
    float _waveLength = ((waveLength.y - waveLength.x) * k + waveLength.x) / filterArea.y;
    float _alpha = (alpha.y - alpha.x) * k + alpha.x;

    float x = vTextureCoord.x + cos(v * 6.28 / _waveLength - time) * _amplitude;
    x = clamp(x, filterClamp.x, filterClamp.z);

    vec4 color = texture2D(uSampler, vec2(x, y));

    gl_FragColor = color * _alpha;
}
`;const V=class extends o.Filter{constructor(t){super(mt,vt),this.time=0,this.uniforms.amplitude=new Float32Array(2),this.uniforms.waveLength=new Float32Array(2),this.uniforms.alpha=new Float32Array(2),this.uniforms.dimensions=new Float32Array(2),Object.assign(this,V.defaults,t)}apply(t,e,r,i){var s,a;this.uniforms.dimensions[0]=(s=e.filterFrame)==null?void 0:s.width,this.uniforms.dimensions[1]=(a=e.filterFrame)==null?void 0:a.height,this.uniforms.time=this.time,t.applyFilter(this,e,r,i)}set mirror(t){this.uniforms.mirror=t}get mirror(){return this.uniforms.mirror}set boundary(t){this.uniforms.boundary=t}get boundary(){return this.uniforms.boundary}set amplitude(t){this.uniforms.amplitude[0]=t[0],this.uniforms.amplitude[1]=t[1]}get amplitude(){return this.uniforms.amplitude}set waveLength(t){this.uniforms.waveLength[0]=t[0],this.uniforms.waveLength[1]=t[1]}get waveLength(){return this.uniforms.waveLength}set alpha(t){this.uniforms.alpha[0]=t[0],this.uniforms.alpha[1]=t[1]}get alpha(){return this.uniforms.alpha}};let N=V;N.defaults={mirror:!0,boundary:.5,amplitude:[0,20],waveLength:[30,100],alpha:[1,1],time:0};var gt=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,xt=`precision mediump float;

varying vec2 vTextureCoord;

uniform sampler2D uSampler;
uniform vec4 filterArea;
uniform vec2 red;
uniform vec2 green;
uniform vec2 blue;

void main(void)
{
   gl_FragColor.r = texture2D(uSampler, vTextureCoord + red/filterArea.xy).r;
   gl_FragColor.g = texture2D(uSampler, vTextureCoord + green/filterArea.xy).g;
   gl_FragColor.b = texture2D(uSampler, vTextureCoord + blue/filterArea.xy).b;
   gl_FragColor.a = texture2D(uSampler, vTextureCoord).a;
}
`;class pt extends o.Filter{constructor(e=[-10,0],r=[0,10],i=[0,0]){super(gt,xt),this.red=e,this.green=r,this.blue=i}get red(){return this.uniforms.red}set red(e){this.uniforms.red=e}get green(){return this.uniforms.green}set green(e){this.uniforms.green=e}get blue(){return this.uniforms.blue}set blue(e){this.uniforms.blue=e}}var yt=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Ct=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;
uniform vec4 filterArea;
uniform vec4 filterClamp;

uniform vec2 center;

uniform float amplitude;
uniform float wavelength;
// uniform float power;
uniform float brightness;
uniform float speed;
uniform float radius;

uniform float time;

const float PI = 3.14159;

void main()
{
    float halfWavelength = wavelength * 0.5 / filterArea.x;
    float maxRadius = radius / filterArea.x;
    float currentRadius = time * speed / filterArea.x;

    float fade = 1.0;

    if (maxRadius > 0.0) {
        if (currentRadius > maxRadius) {
            gl_FragColor = texture2D(uSampler, vTextureCoord);
            return;
        }
        fade = 1.0 - pow(currentRadius / maxRadius, 2.0);
    }

    vec2 dir = vec2(vTextureCoord - center / filterArea.xy);
    dir.y *= filterArea.y / filterArea.x;
    float dist = length(dir);

    if (dist <= 0.0 || dist < currentRadius - halfWavelength || dist > currentRadius + halfWavelength) {
        gl_FragColor = texture2D(uSampler, vTextureCoord);
        return;
    }

    vec2 diffUV = normalize(dir);

    float diff = (dist - currentRadius) / halfWavelength;

    float p = 1.0 - pow(abs(diff), 2.0);

    // float powDiff = diff * pow(p, 2.0) * ( amplitude * fade );
    float powDiff = 1.25 * sin(diff * PI) * p * ( amplitude * fade );

    vec2 offset = diffUV * powDiff / filterArea.xy;

    // Do clamp :
    vec2 coord = vTextureCoord + offset;
    vec2 clampedCoord = clamp(coord, filterClamp.xy, filterClamp.zw);
    vec4 color = texture2D(uSampler, clampedCoord);
    if (coord != clampedCoord) {
        color *= max(0.0, 1.0 - length(coord - clampedCoord));
    }

    // No clamp :
    // gl_FragColor = texture2D(uSampler, vTextureCoord + offset);

    color.rgb *= 1.0 + (brightness - 1.0) * p * fade;

    gl_FragColor = color;
}
`;const X=class extends o.Filter{constructor(t=[0,0],e,r=0){super(yt,Ct),this.center=t,Object.assign(this,X.defaults,e),this.time=r}apply(t,e,r,i){this.uniforms.time=this.time,t.applyFilter(this,e,r,i)}get center(){return this.uniforms.center}set center(t){this.uniforms.center=t}get amplitude(){return this.uniforms.amplitude}set amplitude(t){this.uniforms.amplitude=t}get wavelength(){return this.uniforms.wavelength}set wavelength(t){this.uniforms.wavelength=t}get brightness(){return this.uniforms.brightness}set brightness(t){this.uniforms.brightness=t}get speed(){return this.uniforms.speed}set speed(t){this.uniforms.speed=t}get radius(){return this.uniforms.radius}set radius(t){this.uniforms.radius=t}};let B=X;B.defaults={amplitude:30,wavelength:160,brightness:1,speed:500,radius:-1};var _t=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,bt=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;
uniform sampler2D uLightmap;
uniform vec4 filterArea;
uniform vec2 dimensions;
uniform vec4 ambientColor;
void main() {
    vec4 diffuseColor = texture2D(uSampler, vTextureCoord);
    vec2 lightCoord = (vTextureCoord * filterArea.xy) / dimensions;
    vec4 light = texture2D(uLightmap, lightCoord);
    vec3 ambient = ambientColor.rgb * ambientColor.a;
    vec3 intensity = ambient + light.rgb;
    vec3 finalColor = diffuseColor.rgb * intensity;
    gl_FragColor = vec4(finalColor, diffuseColor.a);
}
`;class St extends o.Filter{constructor(e,r=0,i=1){super(_t,bt),this._color=0,this.uniforms.dimensions=new Float32Array(2),this.uniforms.ambientColor=new Float32Array([0,0,0,i]),this.texture=e,this.color=r}apply(e,r,i,s){var a,l;this.uniforms.dimensions[0]=(a=r.filterFrame)==null?void 0:a.width,this.uniforms.dimensions[1]=(l=r.filterFrame)==null?void 0:l.height,e.applyFilter(this,r,i,s)}get texture(){return this.uniforms.uLightmap}set texture(e){this.uniforms.uLightmap=e}set color(e){const r=this.uniforms.ambientColor;typeof e=="number"?(o.utils.hex2rgb(e,r),this._color=e):(r[0]=e[0],r[1]=e[1],r[2]=e[2],r[3]=e[3],this._color=o.utils.rgb2hex(r))}get color(){return this._color}get alpha(){return this.uniforms.ambientColor[3]}set alpha(e){this.uniforms.ambientColor[3]=e}}var Tt=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Ft=`varying vec2 vTextureCoord;

uniform sampler2D uSampler;
uniform float blur;
uniform float gradientBlur;
uniform vec2 start;
uniform vec2 end;
uniform vec2 delta;
uniform vec2 texSize;

float random(vec3 scale, float seed)
{
    return fract(sin(dot(gl_FragCoord.xyz + seed, scale)) * 43758.5453 + seed);
}

void main(void)
{
    vec4 color = vec4(0.0);
    float total = 0.0;

    float offset = random(vec3(12.9898, 78.233, 151.7182), 0.0);
    vec2 normal = normalize(vec2(start.y - end.y, end.x - start.x));
    float radius = smoothstep(0.0, 1.0, abs(dot(vTextureCoord * texSize - start, normal)) / gradientBlur) * blur;

    for (float t = -30.0; t <= 30.0; t++)
    {
        float percent = (t + offset - 0.5) / 30.0;
        float weight = 1.0 - abs(percent);
        vec4 sample = texture2D(uSampler, vTextureCoord + delta / texSize * percent * radius);
        sample.rgb *= sample.a;
        color += sample * weight;
        total += weight;
    }

    color /= total;
    color.rgb /= color.a + 0.00001;

    gl_FragColor = color;
}
`;class C extends o.Filter{constructor(e=100,r=600,i,s){super(Tt,Ft),this.uniforms.blur=e,this.uniforms.gradientBlur=r,this.uniforms.start=i||new o.Point(0,window.innerHeight/2),this.uniforms.end=s||new o.Point(600,window.innerHeight/2),this.uniforms.delta=new o.Point(30,30),this.uniforms.texSize=new o.Point(window.innerWidth,window.innerHeight),this.updateDelta()}updateDelta(){this.uniforms.delta.x=0,this.uniforms.delta.y=0}get blur(){return this.uniforms.blur}set blur(e){this.uniforms.blur=e}get gradientBlur(){return this.uniforms.gradientBlur}set gradientBlur(e){this.uniforms.gradientBlur=e}get start(){return this.uniforms.start}set start(e){this.uniforms.start=e,this.updateDelta()}get end(){return this.uniforms.end}set end(e){this.uniforms.end=e,this.updateDelta()}}class G extends C{updateDelta(){const e=this.uniforms.end.x-this.uniforms.start.x,r=this.uniforms.end.y-this.uniforms.start.y,i=Math.sqrt(e*e+r*r);this.uniforms.delta.x=e/i,this.uniforms.delta.y=r/i}}class q extends C{updateDelta(){const e=this.uniforms.end.x-this.uniforms.start.x,r=this.uniforms.end.y-this.uniforms.start.y,i=Math.sqrt(e*e+r*r);this.uniforms.delta.x=-r/i,this.uniforms.delta.y=e/i}}class zt extends o.Filter{constructor(e=100,r=600,i,s){super(),this.tiltShiftXFilter=new G(e,r,i,s),this.tiltShiftYFilter=new q(e,r,i,s)}apply(e,r,i,s){const a=e.getFilterTexture();this.tiltShiftXFilter.apply(e,r,a,1),this.tiltShiftYFilter.apply(e,a,i,s),e.returnFilterTexture(a)}get blur(){return this.tiltShiftXFilter.blur}set blur(e){this.tiltShiftXFilter.blur=this.tiltShiftYFilter.blur=e}get gradientBlur(){return this.tiltShiftXFilter.gradientBlur}set gradientBlur(e){this.tiltShiftXFilter.gradientBlur=this.tiltShiftYFilter.gradientBlur=e}get start(){return this.tiltShiftXFilter.start}set start(e){this.tiltShiftXFilter.start=this.tiltShiftYFilter.start=e}get end(){return this.tiltShiftXFilter.end}set end(e){this.tiltShiftXFilter.end=this.tiltShiftYFilter.end=e}}var At=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Pt=`varying vec2 vTextureCoord;

uniform sampler2D uSampler;
uniform float radius;
uniform float angle;
uniform vec2 offset;
uniform vec4 filterArea;

vec2 mapCoord( vec2 coord )
{
    coord *= filterArea.xy;
    coord += filterArea.zw;

    return coord;
}

vec2 unmapCoord( vec2 coord )
{
    coord -= filterArea.zw;
    coord /= filterArea.xy;

    return coord;
}

vec2 twist(vec2 coord)
{
    coord -= offset;

    float dist = length(coord);

    if (dist < radius)
    {
        float ratioDist = (radius - dist) / radius;
        float angleMod = ratioDist * ratioDist * angle;
        float s = sin(angleMod);
        float c = cos(angleMod);
        coord = vec2(coord.x * c - coord.y * s, coord.x * s + coord.y * c);
    }

    coord += offset;

    return coord;
}

void main(void)
{

    vec2 coord = mapCoord(vTextureCoord);

    coord = twist(coord);

    coord = unmapCoord(coord);

    gl_FragColor = texture2D(uSampler, coord );

}
`;const K=class extends o.Filter{constructor(t){super(At,Pt),Object.assign(this,K.defaults,t)}get offset(){return this.uniforms.offset}set offset(t){this.uniforms.offset=t}get radius(){return this.uniforms.radius}set radius(t){this.uniforms.radius=t}get angle(){return this.uniforms.angle}set angle(t){this.uniforms.angle=t}};let W=K;W.defaults={radius:200,angle:4,padding:20,offset:new o.Point};var wt=`attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`,Mt=`varying vec2 vTextureCoord;
uniform sampler2D uSampler;
uniform vec4 filterArea;

uniform vec2 uCenter;
uniform float uStrength;
uniform float uInnerRadius;
uniform float uRadius;

const float MAX_KERNEL_SIZE = \${maxKernelSize};

// author: http://byteblacksmith.com/improvements-to-the-canonical-one-liner-glsl-rand-for-opengl-es-2-0/
highp float rand(vec2 co, float seed) {
    const highp float a = 12.9898, b = 78.233, c = 43758.5453;
    highp float dt = dot(co + seed, vec2(a, b)), sn = mod(dt, 3.14159);
    return fract(sin(sn) * c + seed);
}

void main() {

    float minGradient = uInnerRadius * 0.3;
    float innerRadius = (uInnerRadius + minGradient * 0.5) / filterArea.x;

    float gradient = uRadius * 0.3;
    float radius = (uRadius - gradient * 0.5) / filterArea.x;

    float countLimit = MAX_KERNEL_SIZE;

    vec2 dir = vec2(uCenter.xy / filterArea.xy - vTextureCoord);
    float dist = length(vec2(dir.x, dir.y * filterArea.y / filterArea.x));

    float strength = uStrength;

    float delta = 0.0;
    float gap;
    if (dist < innerRadius) {
        delta = innerRadius - dist;
        gap = minGradient;
    } else if (radius >= 0.0 && dist > radius) { // radius < 0 means it's infinity
        delta = dist - radius;
        gap = gradient;
    }

    if (delta > 0.0) {
        float normalCount = gap / filterArea.x;
        delta = (normalCount - delta) / normalCount;
        countLimit *= delta;
        strength *= delta;
        if (countLimit < 1.0)
        {
            gl_FragColor = texture2D(uSampler, vTextureCoord);
            return;
        }
    }

    // randomize the lookup values to hide the fixed number of samples
    float offset = rand(vTextureCoord, 0.0);

    float total = 0.0;
    vec4 color = vec4(0.0);

    dir *= strength;

    for (float t = 0.0; t < MAX_KERNEL_SIZE; t++) {
        float percent = (t + offset) / MAX_KERNEL_SIZE;
        float weight = 4.0 * (percent - percent * percent);
        vec2 p = vTextureCoord + dir * percent;
        vec4 sample = texture2D(uSampler, p);

        // switch to pre-multiplied alpha to correctly blur transparent images
        // sample.rgb *= sample.a;

        color += sample * weight;
        total += weight;

        if (t > countLimit){
            break;
        }
    }

    color /= total;
    // switch back from pre-multiplied alpha
    // color.rgb /= color.a + 0.00001;

    gl_FragColor = color;
}
`,Y=Object.getOwnPropertySymbols,Dt=Object.prototype.hasOwnProperty,Ot=Object.prototype.propertyIsEnumerable,Rt=(t,e)=>{var r={};for(var i in t)Dt.call(t,i)&&e.indexOf(i)<0&&(r[i]=t[i]);if(t!=null&&Y)for(var i of Y(t))e.indexOf(i)<0&&Ot.call(t,i)&&(r[i]=t[i]);return r};const Z=class extends o.Filter{constructor(t){const e=Object.assign(Z.defaults,t),{maxKernelSize:r}=e,i=Rt(e,["maxKernelSize"]);super(wt,Mt.replace("${maxKernelSize}",r.toFixed(1))),Object.assign(this,i)}get center(){return this.uniforms.uCenter}set center(t){this.uniforms.uCenter=t}get strength(){return this.uniforms.uStrength}set strength(t){this.uniforms.uStrength=t}get innerRadius(){return this.uniforms.uInnerRadius}set innerRadius(t){this.uniforms.uInnerRadius=t}get radius(){return this.uniforms.uRadius}set radius(t){(t<0||t===1/0)&&(t=-1),this.uniforms.uRadius=t}};let U=Z;return U.defaults={strength:.1,center:[0,0],innerRadius:0,radius:-1,maxKernelSize:32},n.AdjustmentFilter=te,n.AdvancedBloomFilter=F,n.AsciiFilter=ce,n.BevelFilter=he,n.BloomFilter=me,n.BulgePinchFilter=A,n.CRTFilter=w,n.ColorMapFilter=ye,n.ColorOverlayFilter=be,n.ColorReplaceFilter=Fe,n.ConvolutionFilter=Pe,n.CrossHatchFilter=De,n.DotFilter=ke,n.DropShadowFilter=R,n.EmbossFilter=Ge,n.GlitchFilter=f,n.GlowFilter=E,n.GodrayFilter=j,n.KawaseBlurFilter=h,n.MotionBlurFilter=et,n.MultiColorReplaceFilter=it,n.OldFilmFilter=I,n.OutlineFilter=y,n.PixelateFilter=ct,n.RGBSplitFilter=pt,n.RadialBlurFilter=ht,n.ReflectionFilter=N,n.ShockwaveFilter=B,n.SimpleLightmapFilter=St,n.TiltShiftAxisFilter=C,n.TiltShiftFilter=zt,n.TiltShiftXFilter=G,n.TiltShiftYFilter=q,n.TwistFilter=W,n.ZoomBlurFilter=U,Object.defineProperty(n,"__esModule",{value:!0}),n}({},PIXI,PIXI.filters,PIXI.filters);Object.assign(PIXI.filters,__filters);
//# sourceMappingURL=pixi-filters.js.map
