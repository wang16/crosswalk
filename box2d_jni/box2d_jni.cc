// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
#include "box2d_jni.h"

#include <map>
#include <string>

#include "Box2D.h"
#include "base/android/jni_string.h"
#include "jni/Box2DExtension_jni.h"

using base::android::ConvertJavaStringToUTF8;
using base::android::ConvertUTF8ToJavaString;

namespace xwalk {
// TODO(nhu): fix the global variables.
namespace {
int g_next_world_id = 0;
int g_next_body_id = 0;
int g_next_fixture_id = 0;
std::map<int, b2World*> g_world_map;
std::map<int, b2Body*> g_body_map;
std::map<int, b2Fixture*> g_fixture_map;

static b2World* getWorldById(int id, bool erase=false) {
  b2World* world = NULL;
  std::map<int, b2World*>::iterator itr = g_world_map.find(id);
  if (itr != g_world_map.end()) {
    world = itr->second;
    if (erase)
      g_world_map.erase(itr);
  }
  return world;
}

static b2Body* getBodyById(int id, bool erase = false) {
  b2Body* body = NULL;
  std::map<int, b2Body*>::iterator itr = g_body_map.find(id);
  if (itr != g_body_map.end()) {
    body = itr->second;
    if (erase)
      g_body_map.erase(itr);
  }
  return body;
}

static b2Fixture* getFixtureById(int id, bool erase = false) {
  b2Fixture* fixture = NULL;
  std::map<int, b2Fixture*>::iterator itr = g_fixture_map.find(id);
  if (itr != g_fixture_map.end()) {
    fixture = itr->second;
    if (erase)
      g_fixture_map.erase(itr);
  }
  return fixture;
}
}  // namespace

namespace box2d {

jint CreateWorld(JNIEnv* env, jobject jcaller,
    jdouble x,
    jdouble y,
    jboolean do_sleep) {
  b2Vec2 gravity(x, y);
  b2World* world = new b2World(gravity);
  int id = g_next_world_id++;
  g_world_map.insert(std::make_pair<int, b2World*>(id, world));
  world->SetAllowSleeping(do_sleep);
  return id;
}

void DeleteBody(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id) {
  b2World* world = getWorldById(world_id);
  b2Body* body = getBodyById(body_id, true);
  if (!world || !body)
    return;
  delete reinterpret_cast<int*>(body->GetUserData());
  world->DestroyBody(body);
}

void CreateDistanceJoint(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_a_id,
    jint body_b_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  b2Body* body_a = getBodyById(body_a_id);
  if (!body_a)
    return;
  b2Body* body_b = getBodyById(body_b_id);
  if (!body_b)
    return;

  b2JointDef joint_def;
  joint_def.type = e_distanceJoint;
  joint_def.bodyA = body_a;
  joint_def.bodyB = body_b;

  world->CreateJoint(&joint_def);
}

void SetContinuous(JNIEnv* env, jobject jcaller,
    jint world_id,
    jboolean continuous) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  world->SetContinuousPhysics(continuous == JNI_TRUE);
}

void SetGravity(JNIEnv* env, jobject jcaller,
    jint world_id,
    jdouble x,
    jdouble y) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  b2Vec2 gravity;
  gravity.x = x;
  gravity.y = y;
  world->SetGravity(gravity);
  return;
}

jstring Step(JNIEnv* env, jobject jcaller,
    jint world_id,
    jdouble dt,
    jint velocity_iterations,
    jint position_iterations) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return NULL;
  world->Step(dt, velocity_iterations, position_iterations);
  b2Body* next_body = world->GetBodyList();
  next_body = world->GetBodyList();
  Java_Box2DExtension_createArray(env, jcaller);
  std::string body_array = "";
  int count = 0;
  while(next_body) {
    char buff[128];
    int body_id =
        *(reinterpret_cast<int*>(next_body->GetUserData()));
    float32 x = next_body->GetPosition().x;
    float32 y = next_body->GetPosition().y;
    float32 angle = next_body->GetAngle();
    sprintf(buff, ",%d,%.6f,%.6f,%.6f", body_id, x, y, angle);
    body_array = body_array + buff;
    next_body = next_body->GetNext();
    count ++;
  }
  char buff[20];
  sprintf(buff, "{\"data\": [%d", count);
  body_array = buff + body_array + "]}";
  return env->NewStringUTF(body_array.c_str());
}

void GetLastContacts(JNIEnv* env, jobject jcaller,
    jint world_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  b2Contact* next_contact = world->GetContactList();
  Java_Box2DExtension_createArray(env, jcaller);
  while(next_contact) {
    b2Fixture * fa = next_contact->GetFixtureA();
    b2Fixture * fb = next_contact->GetFixtureB();
    int fa_id = *(reinterpret_cast<int*>(fa->GetUserData()));
    int fb_id = *(reinterpret_cast<int*>(fb->GetUserData()));
    bool is_touching = next_contact->IsTouching();
    Java_Box2DExtension_createContact(
        env, jcaller, fa_id, fb_id, is_touching ? JNI_TRUE : JNI_FALSE);
    next_contact = next_contact->GetNext();
  }
}

void ClearForces(JNIEnv* env, jobject jcaller,
    jint world_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  world->ClearForces();
}

void SetSensor(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint fixture_id,
    jboolean is_sensor) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  b2Fixture* fixture = getFixtureById(fixture_id);
  if (!fixture)
    return;
  fixture->SetSensor(is_sensor == JNI_TRUE);
}

void SetDensity(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint fixture_id,
    jfloat density) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  b2Fixture* fixture = getFixtureById(fixture_id);
  if (!fixture)
    return;
  fixture->SetDensity(density);
}

void SetFriction(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint fixture_id,
    jfloat friction) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  b2Fixture* fixture = getFixtureById(fixture_id);
  if (!fixture)
    return;
  fixture->SetFriction(friction);
}

void SetRestitution(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint fixture_id,
    jfloat restitution) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  b2Fixture* fixture = getFixtureById(fixture_id);
  if (!fixture)
    return;
  fixture->SetRestitution(restitution);
}

jint CreateBody(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint type,
    jdouble x,
    jdouble y) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return -1;

  // TODO(nhu): need to handle other parameters
  b2BodyDef body_def;
  body_def.type = static_cast<b2BodyType>(type);
  body_def.position.Set(x, y);

  b2Body* body = world->CreateBody(&body_def);
  int* id = new int;
  *id = g_next_body_id++;
  body->SetUserData(reinterpret_cast<void*>(id));

  g_body_map.insert(std::make_pair<int, b2Body*>(*id, body));

  return *id;
}

jint CreateFixture(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id,
    jdouble friction,
    jdouble restitution,
    jdouble density,
    jstring jtype,
    jdouble param1,
    jdouble param2) {
  b2Shape* shape = NULL;
  std::string type = ConvertJavaStringToUTF8(env, jtype);
  if (type == std::string("circle")) {
    double radius = param1;
    shape = new b2CircleShape();
    shape->m_radius = param1;
  } else if (type == std::string("box")) {
    double width = param1;
    double height = param2;
    shape = new b2PolygonShape();
    static_cast<b2PolygonShape*>(shape)->SetAsBox(width/2, height/2);
  } else if (type == std::string("edge")) {
  } else if (type == std::string("polygon")) {
  } else {
  }

  b2World* world = getWorldById(world_id);
  if (!world) {
    delete shape;
    return -1;
  }

  b2Body* body = getBodyById(body_id);
  if (!body) {
    delete shape;
    return -1;
  }

  // TODO(nhu): handle other parameters
  b2FixtureDef fixture_def;
  fixture_def.shape = shape;
  fixture_def.friction = friction;
  fixture_def.restitution = restitution;
  fixture_def.density = density;

  b2Fixture* fixture = body->CreateFixture(&fixture_def);

  int* fixture_id = new int;
  *fixture_id = g_next_fixture_id++;
  fixture->SetUserData(reinterpret_cast<void*>(fixture_id));

  g_fixture_map.insert(
      std::make_pair<int, b2Fixture*>(*fixture_id, fixture));

  if (shape)
    delete shape;
  return *fixture_id;
}

void SetBodyTransform(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id,
    jdouble x,
    jdouble y,
    jdouble angle) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  b2Vec2 position(x, y);
  body->SetTransform(position, angle);
}

void GetLinearVelocity(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  b2Vec2 v = body->GetLinearVelocity();

  Java_Box2DExtension_returnPoint(env, jcaller, v.x, v.y);
}

void GetWorldCenter(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  b2Vec2 p = body->GetWorldCenter();

  Java_Box2DExtension_returnPoint(env, jcaller, p.x, p.y);
}

void GetLocalCenter(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  b2Vec2 p = body->GetLocalCenter();

  Java_Box2DExtension_returnPoint(env, jcaller, p.x, p.y);
}

void ApplyImpulse(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id,
    jdouble impluse_x,
    jdouble impluse_y,
    jdouble point_x,
    jdouble point_y,
    jboolean wake) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  b2Vec2 impluse(impluse_x, impluse_y);
  b2Vec2 point(point_x, point_y);
  body->ApplyLinearImpulse(impluse, point, wake == JNI_TRUE);
}

jboolean IsAwake(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return false;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return false;

  bool wake = body->IsAwake();
  return wake ? JNI_TRUE : JNI_FALSE;
}

jfloat GetAngularVelocity(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return 0;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return 0;

  float v = body->GetAngularVelocity();
  return v;
}

void SetAwake(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id,
    jboolean wake) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  body->SetAwake(wake == JNI_TRUE);
}

void SetLinearVelocity(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id,
    jdouble x,
    jdouble y) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  b2Vec2 v(x, y);
  body->SetLinearVelocity(v);
}

void ApplyForceToCenter(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id,
    jdouble x,
    jdouble y,
    jboolean wake) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  b2Vec2 force(x, y);
  body->ApplyForceToCenter(force, wake == JNI_TRUE);
}

void SetLinearDamping(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id,
    jdouble damp) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  body->SetLinearDamping(damp);
}

void SetAngularVelocity(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id,
    jdouble w) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  body->SetAngularVelocity(w);
}

void SetActive(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id,
    jboolean active) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  body->SetActive(active == JNI_TRUE);
}

void GetObjectContacts(JNIEnv* env, jobject jcaller,
    jint world_id,
    jint body_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;
  
  b2ContactEdge* next_edge = body->GetContactList();
  Java_Box2DExtension_createArray(env, jcaller);

  while(next_edge) {
    b2Body* other = next_edge->other;
    int other_id =
        *(reinterpret_cast<int*>(other->GetUserData()));
    Java_Box2DExtension_createInteger(env, jcaller, other_id);
    next_edge = next_edge->next;
  }
}

}  // namespace box2d

bool RegisterXWalkBox2DExtension(JNIEnv* env) {
  return box2d::RegisterNativesImpl(env) >= 0; 
}

}  // namespace xwalk
