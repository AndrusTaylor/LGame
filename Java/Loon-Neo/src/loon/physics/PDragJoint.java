/**
 * Copyright 2013 The Loon Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package loon.physics;

import loon.geom.Vector2f;
import loon.utils.MathUtils;

public class PDragJoint extends PJoint {

	private Vector2f anchor;
	
	private PBody bodyObject;
	
	private Vector2f dragPoint;
	
	private Vector2f localAnchor;
	
	private PTransformer mass;
	
	private Vector2f relAnchor;

	public PDragJoint(PBody b, float px, float py) {
		this.bodyObject = b;
		dragPoint = new Vector2f(px, py);
		localAnchor = new Vector2f(px - b.pos.x, py - b.pos.y);
		b.mAng.transpose().mulEqual(localAnchor);
		anchor = b.mAng.mul(localAnchor);
		anchor.addSelf(b.pos);
		type = PJointType.DRAG_JOINT;
		mass = new PTransformer();
	}

	public Vector2f getAnchorPoint() {
		return anchor.cpy();
	}

	public PBody getBody() {
		return bodyObject;
	}

	public Vector2f getDragPoint() {
		return dragPoint.cpy();
	}

	public Vector2f getRelativeAnchorPoint() {
		return relAnchor.cpy();
	}

	void preSolve(float dt) {
		relAnchor = bodyObject.mAng.mul(localAnchor);
		anchor.set(relAnchor.x + bodyObject.pos.x, relAnchor.y + bodyObject.pos.y);
		mass = PTransformer.calcEffectiveMass(bodyObject, relAnchor);
		Vector2f f = anchor.sub(dragPoint);
		float k = bodyObject.m;
		f.mulLocal(-k * 20F);
		Vector2f relVel = bodyObject.vel.cpy();
		relVel.x += -bodyObject.angVel * relAnchor.y;
		relVel.y += bodyObject.angVel * relAnchor.x;
		relVel.mulLocal(MathUtils.sqrt(k * 20F * k));
		f.subLocal(relVel);
		f.mulLocal(dt);
		mass.mulEqual(f);
		bodyObject.applyImpulse(f.x, f.y, anchor.x, anchor.y);
	}

	public void setDragPosition(float px, float py) {
		dragPoint.set(px, py);
	}

	public void setRelativeAnchorPoint(float relx, float rely) {
		localAnchor.set(relx, rely);
		bodyObject.mAng.transpose().mulEqual(localAnchor);
	}

	void solvePosition() {
	}

	void solveVelocity(float f) {
	}

	void update() {
		relAnchor = bodyObject.mAng.mul(localAnchor);
		anchor.set(relAnchor.x + bodyObject.pos.x, relAnchor.y + bodyObject.pos.y);
		if (bodyObject.rem || bodyObject.fix) rem = true;
	}

}
