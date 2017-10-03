#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform sampler2D m_texture;
uniform float m_alpha;

varying vec2 v_texCoord;
varying vec4 v_color;

void main() {
	vec4 v4Colour = texture2D(u_texture, v_texCoord);
    //v4Colour.a = texture2D(m_texture, v_texCoord).r;
    if(m_alpha>=1.0){
    	v4Colour.a = texture2D(m_texture, v_texCoord).r;
    }
    else{
        v4Colour.a =texture2D(m_texture, v_texCoord).r*m_alpha;
    }
    gl_FragColor = v4Colour;
}