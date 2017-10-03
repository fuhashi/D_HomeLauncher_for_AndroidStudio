package jp.co.disney.apps.managed.kisekaeapp.catalog.screens;

import com.badlogic.gdx.math.MathUtils;

public class EaseMoving {
    private float startPos;
//    private float goalPos;
    private float duration;
    private float distance;

    private float stateTime = 0.0f;
    private float alpha;
  private float backValue = 1.70158f;//反動距離的な　大きくすると距離が伸びる
//フラグTRUEなら終了時間ショートカット
  private boolean easeOutElastic =false;

  public boolean isFinishAnim(){
	  if(stateTime>=duration-0.01f) return true;
	  if(easeOutElastic) if(stateTime>=duration*0.6f) return true;
	  return false;
  }

  public EaseMoving() {
      this.startPos = 0f;
//      this.goalPos = goalPos;
      this.duration = 0f;
      this.distance = 0f;
      this.backValue = 0f;
  }

    public EaseMoving(final float startPos, final float goalPos, float duration, float backvalue) {
        this.startPos = startPos;
//        this.goalPos = goalPos;
        this.duration = duration;
        this.distance = goalPos-startPos;
        this.backValue = backvalue;
    }

    public EaseMoving(final float startPos, final float goalPos, float duration) {
        this.startPos = startPos;
//        this.goalPos = goalPos;
        this.duration = duration;
        this.distance = goalPos-startPos;
    }
//    public void ResetPosition(final float startPos, final float goalPos, float duration) {
//        this.startPos = startPos;
////        this.goalPos = goalPos;
//        this.duration = duration;
//        this.distance = goalPos-startPos;
//        stateTime = 0.0f;
//    }
    public void ResetPosition(final float startPos, final float goalPos, float duration) {
    	ResetPosition(startPos, goalPos, duration,false);
    }
    public void ResetPosition(final float startPos, final float goalPos, float duration,boolean easeOutElastic) {
        this.startPos = startPos;
//        this.goalPos = goalPos;
        this.duration = duration;
        this.distance = goalPos-startPos;
        this.easeOutElastic = easeOutElastic;
        stateTime = 0.0f;
    }
    public void ResetTime() {
        stateTime = 0.0f;
    }
    public void SetStateTime(float time) {
        stateTime = time;
    }
    //cubic
    public float actEOUT(float delta) {
    	stateTime += delta;
        alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
//        alpha = stateTime/duration;
        alpha--;
        return distance*(alpha*alpha*alpha+ 1) + startPos;
    }
    public float actEIN(float delta) {
    	stateTime += delta;
      alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
//      alpha = stateTime/duration;
        return distance*alpha*alpha*alpha + startPos;
    }
	public float actEINOUT(float delta) {
    	stateTime += delta;
    	alpha = MathUtils.clamp(stateTime/duration/2f, 0.0f,2.0f);
    	if(alpha< 1)return distance/2f*alpha*alpha*alpha + startPos;
        alpha-=2f;
        return distance/2f*(alpha*alpha*alpha+2f) + startPos;
	}
//    public float actEINOUT(float delta) {
//    	stateTime += delta;
//        alpha = stateTime/duration/2.0f;
//        if(alpha< 1) return distance/2f*alpha*alpha*alpha + startPos;
//        alpha-=2f;
//        return distance/2f*(alpha*alpha*alpha+2f) + startPos;
//    }
    //Quadratic
     public float actEINQuadratic(float delta) {
        stateTime += delta;
        alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
        return distance*alpha*alpha + startPos;
     }
     public float actEINCubic(float delta){
 		stateTime += delta;
 		alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
    	return distance * alpha * alpha * alpha + startPos;
     };
     public float easeInQuart(float delta){
  		stateTime += delta;
  		alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
 		return distance* alpha * alpha * alpha * alpha + startPos;
 	}
     public float actEINExpo(float delta) {
    		stateTime += delta;
    		alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
    		 return (float) (distance * Math.pow(2, 10 * (alpha - 1)) + startPos);
    }
    public float actEOUTQuadratic(float delta) {
    	stateTime += delta;
    	alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
//      alpha = stateTime/duration;
        return -distance*alpha*(alpha-2.0f) + startPos;
    }
//    return -c * ((t = t / d - 1f) * t * t * t - 1f) + b;
    public float actEOUTQuart(float delta) {
    	stateTime += delta;
        alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
		return -distance * ((alpha-1f)* (alpha-1f) * (alpha-1f) * (alpha-1f)- 1f) + startPos;
	}
	public float actEINOUTQuadratic(float delta) {
    	stateTime += delta;
    	alpha = MathUtils.clamp(stateTime/duration/2f, 0.0f,2.0f);
    	if(alpha< 1) return distance/2f*alpha*alpha + startPos;
        alpha-=1f;
        return -distance/2f*(alpha*(alpha-2f)-1f) + startPos;
	}
//    public float actEINOUTQuadratic(float delta) {
//    	stateTime += delta;
//    	alpha = stateTime/duration/2.0f;
//        if(alpha< 1) return distance/2f*alpha*alpha + startPos;
//        alpha-=1f;
//        return -distance/2f*(alpha*(alpha-2f)-1f) + startPos;
//    }
    /*
    引数はそれぞれ以下を設定。
    t : 0～1
    b : 0
    c : 100
    d : 1
    因みに、引数はそれぞれ以下のデータのなります。
    t : 時間(進行度)
    b : 開始の値(開始時の座標やスケールなど)
    c : 開始と終了の値の差分
    d : Tween(トゥイーン)の合計時間

    def ease_in(t, b, c, d):
	t /= d
	return c*t*t*t + b

	  public float actEIN(float delta) {
    	stateTime += delta;
      alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
//      alpha = stateTime/duration;
        return distance*alpha*alpha*alpha + startPos;
    }

*/

    public float actEaseInBack(float delta) {
    	stateTime += delta;
    	alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
		return distance * alpha * alpha* ((backValue + 1f) * alpha - backValue) + startPos;
	}
    public float actEaseOutBack(float delta) {
    	stateTime += delta;
    	alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
    	alpha--;
		return distance * (alpha * alpha * ((backValue + 1f) * alpha + backValue) + 1f) + startPos;
	}
    public float actEaseOutBack(float delta,float localBackValue) {
    	stateTime += delta;
    	alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
    	alpha--;
		return distance * (alpha * alpha * ((localBackValue + 1f) * alpha + localBackValue) + 1f) + startPos;
	}

    public float actEaseOutBounce(float delta) {
    	stateTime += delta;
    	alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
    	if (alpha < (1f / 2.75f)) {
			return distance * (7.5625f * alpha * alpha) + startPos;
		} else if (alpha < (2f / 2.75f)) {
			return distance * (7.5625f * (alpha -= (1.5f / 2.75f)) * alpha + 0.75f) + startPos;
		} else if (alpha < (2.5f / 2.75f)) {
			return distance * (7.5625f * (alpha -= (2.25f / 2.75f)) * alpha + 0.9375f) + startPos;
		} else {
			return distance * (7.5625f * (alpha -= (2.625f / 2.75f)) * alpha + 0.984375f) + startPos;
		}
	}
//  t = stateTime
// d = duration
// b=startPos
// c  = distance
// (t /= d) = alpha
//     s = backValue
    public static float easeInElastic(float t, float b, float c, float d) {
 		float s = 1.70158f;
 		float p = 0;
 		float a = c;
 		if (t == 0)
 			return b;
 		if ((t /= d) == 1)
 			return b + c;
 		p = d * 0.3f;
 		if (a < Math.abs(c)) {
 			a = c;
 			s = p / 4f;
 		} else
 			s = (float) (p / (2f * Math.PI) * Math.asin(c / a));
 		return (float) (-(a * Math.pow(2, 10f * (t -= 1f)) * Math.sin((t * d - s) * (2f * Math.PI) / p)) + b);
 	}

//とりあえずやってみた…
// 	public float actEaseOutElastic(float delta) {
//    	stateTime += delta;
//    	alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
// 		float s = 1.70158f;
// 		float p = 0;
// 		float a = distance;
// 		if (alpha == 0)
// 			return startPos;
// 		if (alpha == 1)
// 			return startPos + distance;
//// 		p = d * 0.3f;
// 		p = duration * 0.3f;
// 		if (a < Math.abs(distance)) {
// 			a = distance;
// 			s = p / 4f;
// 		} else
// 			s = (float) (p / (2f * Math.PI) * Math.asin(distance / a));
//// 		return (float) (a * Math.pow(2, -10f * t) * Math.sin((t * d - s) * (2f * Math.PI) / p) + distance + startPos);
// 		return (float) (a * Math.pow(2, -10f * alpha) * Math.sin((alpha * duration - s) * (2f * Math.PI) / p) + distance + startPos);
// 	}
 	public float actEaseOutElastic(float delta) {
    	stateTime += delta;
    	alpha = MathUtils.clamp(stateTime/duration, 0.0f,1.0f);
 		float s = 1.70158f;
 		float p = 0;
// 		float a = distance;
 		if (alpha == 0)
 			return startPos;
 		if (alpha == 1)
 			return startPos + distance;
// 		p = duration * 0.3f;
 		p = duration * 0.3f;
// 		if (a < Math.abs(distance)) {
// 			a = distance;
// 			s = p / 4f;
// 		} else
 		s = p / 4f;
// 		s = (float) (p / (2f * Math.PI) * Math.asin(distance / distance));
 		return (float) (distance * Math.pow(2, -10f * alpha) * Math.sin((alpha * duration - s) * (2f * Math.PI) / p) + distance + startPos);
 	}
    public float getStatetime(){
    	return stateTime;
    }

}