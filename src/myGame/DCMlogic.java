package myGame;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class DCMlogic {

	private final Object sincronizzaUpdate = new Object();

	float sampleFreq = 100;
	float twoKpDef = (2.0f * 5f);
	public float q0 = 1, q1 = 0, q2 = 0, q3 = 0;
	float twoKp = twoKpDef;
	float LSBtoRadiant = 802.1409132f;
	
	Vector3f gyro=new Vector3f(), acc=new Vector3f(), magn=new Vector3f();
	
	
	//long lastUp = System.nanoTime();
	long count=-1, lastFreqUp=System.currentTimeMillis();

	private float	qPred1;

	private float	qPred2;

	private float	qPred3;

	private float	qPred4;
	void MadgwickAHRSupdate(float gx, float gy, float gz, float ax, float ay, float az, float mx, float my, float mz) {
		if (count == -1){ //just the first time!
			lastFreqUp = System.currentTimeMillis();
		}
		count ++;
		if (System.currentTimeMillis()-lastFreqUp>=1000){
			System.out.println("Frequenza: "+count );
			sampleFreq = count;
			count=0;
			lastFreqUp = System.currentTimeMillis();
			/*
			if (sampleFreq < 70){
				System.exit(0);
			}
			*/
		}
		/*
		sampleFreq = 1000000000L/(System.nanoTime()-lastUp);
		System.out.println("Frequenza relativa: "+sampleFreq+" intervallo: "+(System.nanoTime()-lastUp) );
		lastUp=System.nanoTime();
		*/
		synchronized (sincronizzaUpdate) {
			float recipNorm;
			float s0, s1, s2, s3;
			float qDot1, qDot2, qDot3, qDot4;
			float hx, hy;
			float _2q0mx, _2q0my, _2q0mz, _2q1mx, _2bx, _2bz, _4bx, _4bz, _2q0, _2q1, _2q2, _2q3, _2q0q2, _2q2q3, q0q0, q0q1, q0q2, q0q3, q1q1, q1q2, q1q3, q2q2, q2q3, q3q3;

			
			gx = gx / LSBtoRadiant;
			gy = gy / LSBtoRadiant;
			gz = gz / LSBtoRadiant;
			
			gyro = gyro.add( new Vector3f(gx, gy, gz).mult(1.0f/sampleFreq) );//lol, simple integration?
			//gyro = new Vector3f(gx, gy, gz).mult(1.0f/sampleFreq);

			acc = new Vector3f(ax, ay, az);

			magn = new Vector3f(mx, my, mz);

			// Use IMU algorithm if magnetometer measurement invalid (avoids NaN
			// in magnetometer normalisation)
			if ((mx == 0.0f) && (my == 0.0f) && (mz == 0.0f)) {
				MadgwickAHRSupdateIMU(gx, gy, gz, ax, ay, az);
				return;
			}

			// Rate of change of quaternion from gyroscope
			qDot1 = 0.5f * (-q1 * gx - q2 * gy - q3 * gz);
			qDot2 = 0.5f * (q0 * gx + q2 * gz - q3 * gy);
			qDot3 = 0.5f * (q0 * gy - q1 * gz + q3 * gx);
			qDot4 = 0.5f * (q0 * gz + q1 * gy - q2 * gx);

			// Compute feedback only if accelerometer measurement valid (avoids
			// NaN in accelerometer normalisation)
			if (!((ax == 0.0f) && (ay == 0.0f) && (az == 0.0f))) {

				// Normalise accelerometer measurement
				recipNorm = invSqrt(ax * ax + ay * ay + az * az);
				ax *= recipNorm;
				ay *= recipNorm;
				az *= recipNorm;

				// Normalise magnetometer measurement
				recipNorm = invSqrt(mx * mx + my * my + mz * mz);
				mx *= recipNorm;
				my *= recipNorm;
				mz *= recipNorm;

				// Auxiliary variables to avoid repeated arithmetic
				_2q0mx = 2.0f * q0 * mx;
				_2q0my = 2.0f * q0 * my;
				_2q0mz = 2.0f * q0 * mz;
				_2q1mx = 2.0f * q1 * mx;
				_2q0 = 2.0f * q0;
				_2q1 = 2.0f * q1;
				_2q2 = 2.0f * q2;
				_2q3 = 2.0f * q3;
				_2q0q2 = 2.0f * q0 * q2;
				_2q2q3 = 2.0f * q2 * q3;
				q0q0 = q0 * q0;
				q0q1 = q0 * q1;
				q0q2 = q0 * q2;
				q0q3 = q0 * q3;
				q1q1 = q1 * q1;
				q1q2 = q1 * q2;
				q1q3 = q1 * q3;
				q2q2 = q2 * q2;
				q2q3 = q2 * q3;
				q3q3 = q3 * q3;

				// Reference direction of Earth's magnetic field
				hx = mx * q0q0 - _2q0my * q3 + _2q0mz * q2 + mx * q1q1 + _2q1
						* my * q2 + _2q1 * mz * q3 - mx * q2q2 - mx * q3q3;
				hy = _2q0mx * q3 + my * q0q0 - _2q0mz * q1 + _2q1mx * q2 - my
						* q1q1 + my * q2q2 + _2q2 * mz * q3 - my * q3q3;
				_2bx = FastMath.sqrt(hx * hx + hy * hy);
				_2bz = -_2q0mx * q2 + _2q0my * q1 + mz * q0q0 + _2q1mx * q3
						- mz * q1q1 + _2q2 * my * q3 - mz * q2q2 + mz * q3q3;
				_4bx = 2.0f * _2bx;
				_4bz = 2.0f * _2bz;

				// Gradient decent algorithm corrective step
				s0 = -_2q2
						* (2.0f * q1q3 - _2q0q2 - ax)
						+ _2q1
						* (2.0f * q0q1 + _2q2q3 - ay)
						- _2bz
						* q2
						* (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx)
						+ (-_2bx * q3 + _2bz * q1)
						* (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my)
						+ _2bx
						* q2
						* (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
				s1 = _2q3
						* (2.0f * q1q3 - _2q0q2 - ax)
						+ _2q0
						* (2.0f * q0q1 + _2q2q3 - ay)
						- 4.0f
						* q1
						* (1 - 2.0f * q1q1 - 2.0f * q2q2 - az)
						+ _2bz
						* q3
						* (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx)
						+ (_2bx * q2 + _2bz * q0)
						* (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my)
						+ (_2bx * q3 - _4bz * q1)
						* (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
				s2 = -_2q0
						* (2.0f * q1q3 - _2q0q2 - ax)
						+ _2q3
						* (2.0f * q0q1 + _2q2q3 - ay)
						- 4.0f
						* q2
						* (1 - 2.0f * q1q1 - 2.0f * q2q2 - az)
						+ (-_4bx * q2 - _2bz * q0)
						* (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx)
						+ (_2bx * q1 + _2bz * q3)
						* (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my)
						+ (_2bx * q0 - _4bz * q2)
						* (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
				s3 = _2q1
						* (2.0f * q1q3 - _2q0q2 - ax)
						+ _2q2
						* (2.0f * q0q1 + _2q2q3 - ay)
						+ (-_4bx * q3 + _2bz * q1)
						* (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx)
						+ (-_2bx * q0 + _2bz * q2)
						* (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my)
						+ _2bx
						* q1
						* (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);

				recipNorm = invSqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalise
																			// step
																			// magnitude
				s0 *= recipNorm;
				s1 *= recipNorm;
				s2 *= recipNorm;
				s3 *= recipNorm;

				// Apply feedback step
				qDot1 -= twoKp * s0;
				qDot2 -= twoKp * s1;
				qDot3 -= twoKp * s2;
				qDot4 -= twoKp * s3;
			}

			// Integrate rate of change of quaternion to yield quaternion
			q0 += qDot1 * (1.0f / sampleFreq);
			q1 += qDot2 * (1.0f / sampleFreq);
			q2 += qDot3 * (1.0f / sampleFreq);
			q3 += qDot4 * (1.0f / sampleFreq);

			// Normalise quaternion
			recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
			q0 *= recipNorm;
			q1 *= recipNorm;
			q2 *= recipNorm;
			q3 *= recipNorm;
		}
	}

	// ---------------------------------------------------------------------------------------------------
	// IMU algorithm update

	void MadgwickAHRSupdateIMU(float gx, float gy, float gz, float ax,
			float ay, float az) {
		float recipNorm;
		float s0, s1, s2, s3;
		float qDot1, qDot2, qDot3, qDot4;
		float _2q0, _2q1, _2q2, _2q3, _4q0, _4q1, _4q2, _8q1, _8q2, q0q0, q1q1, q2q2, q3q3;

		// Rate of change of quaternion from gyroscope
		qDot1 = 0.5f * (-q1 * gx - q2 * gy - q3 * gz);
		qDot2 = 0.5f * (q0 * gx + q2 * gz - q3 * gy);
		qDot3 = 0.5f * (q0 * gy - q1 * gz + q3 * gx);
		qDot4 = 0.5f * (q0 * gz + q1 * gy - q2 * gx);

		// Compute feedback only if accelerometer measurement valid (avoids NaN
		// in accelerometer normalisation)
		if (!((ax == 0.0f) && (ay == 0.0f) && (az == 0.0f))) {

			// Normalise accelerometer measurement
			recipNorm = invSqrt(ax * ax + ay * ay + az * az);
			ax *= recipNorm;
			ay *= recipNorm;
			az *= recipNorm;

			// Auxiliary variables to avoid repeated arithmetic
			_2q0 = 2.0f * q0;
			_2q1 = 2.0f * q1;
			_2q2 = 2.0f * q2;
			_2q3 = 2.0f * q3;
			_4q0 = 4.0f * q0;
			_4q1 = 4.0f * q1;
			_4q2 = 4.0f * q2;
			_8q1 = 8.0f * q1;
			_8q2 = 8.0f * q2;
			q0q0 = q0 * q0;
			q1q1 = q1 * q1;
			q2q2 = q2 * q2;
			q3q3 = q3 * q3;

			// Gradient decent algorithm corrective step
			s0 = _4q0 * q2q2 + _2q2 * ax + _4q0 * q1q1 - _2q1 * ay;
			s1 = _4q1 * q3q3 - _2q3 * ax + 4.0f * q0q0 * q1 - _2q0 * ay - _4q1
					+ _8q1 * q1q1 + _8q1 * q2q2 + _4q1 * az;
			s2 = 4.0f * q0q0 * q2 + _2q0 * ax + _4q2 * q3q3 - _2q3 * ay - _4q2
					+ _8q2 * q1q1 + _8q2 * q2q2 + _4q2 * az;
			s3 = 4.0f * q1q1 * q3 - _2q1 * ax + 4.0f * q2q2 * q3 - _2q2 * ay;
			recipNorm = invSqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalise
																		// step
																		// magnitude
			s0 *= recipNorm;
			s1 *= recipNorm;
			s2 *= recipNorm;
			s3 *= recipNorm;

			qPred1 = qDot1 + s0;
			qPred2 = qDot1 + s0;
			qPred3 = qDot1 + s0;
			qPred4 = qDot1 + s0;
			
			// Apply feedback step
			qDot1 -= twoKp * s0;
			qDot2 -= twoKp * s1;
			qDot3 -= twoKp * s2;
			qDot4 -= twoKp * s3;			
			
		}

		// Integrate rate of change of quaternion to yield quaternion
		q0 += qDot1 * (1.0f / sampleFreq);
		q1 += qDot2 * (1.0f / sampleFreq);
		q2 += qDot3 * (1.0f / sampleFreq);
		q3 += qDot4 * (1.0f / sampleFreq);

		// Normalise quaternion
		recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
		q0 *= recipNorm;
		q1 *= recipNorm;
		q2 *= recipNorm;
		q3 *= recipNorm;
	}

	// ---------------------------------------------------------------------------------------------------
	// Fast inverse square-root
	// See: http://en.wikipedia.org/wiki/Fast_inverse_square_root
	// also this is needed because otherwise we could get a NAN (it's also a bit
	// faster i guess)
	strictfp static float invSqrt(float x) {
		float xhalf = 0.5f * x;
		int i = Float.floatToRawIntBits(x); // convert integer to keep the
											// representation IEEE 754

		i = 0x5f3759df - (i >> 1);
		x = Float.intBitsToFloat(i);
		x = x * (1.5f - xhalf * x * x);

		return x;
	}

	public Quaternion getQuaternion() {
		synchronized (sincronizzaUpdate) {
			return new Quaternion(q0, q1, q2, q3);
		}
	}
	
	public Quaternion getPredictedQuaternion() {
		synchronized (sincronizzaUpdate) {
			return new Quaternion(qPred1, qPred2, qPred3, qPred4);
		}
	}
	
	public Vector3f getGyro() {
		synchronized (sincronizzaUpdate) {
			return new Vector3f(gyro); //copy!
		}
	}
	
	public Vector3f getAcc() {
		synchronized (sincronizzaUpdate) {
			return new Vector3f(acc); //copy!
		}
	}
	
	public Vector3f getMagn() {
		synchronized (sincronizzaUpdate) {
			return new Vector3f(magn); //copy!
		}
	}
}