// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include <jni.h>
#include <map>
#include <string>

#include "Box2D.h"

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

jint Java_org_xwalk_core_extension_api_box2d_Box2DExtension_createWorld(
    JNIEnv * env, jobject thiz, jdouble x, jdouble y, jboolean do_sleep) {
  b2Vec2 gravity(x, y);
  b2World* world = new b2World(gravity);
  int id = g_next_world_id++;
  g_world_map.insert(std::make_pair<int, b2World*>(id, world));
  world->SetAllowSleeping(do_sleep);
  return id;
}

void deleteBody(int world_id, int body_id) {
  b2World* world = getWorldById(world_id);
  b2Body* body = getBodyById(body_id, true);
  if (!world || !body)
    return;
  delete reinterpret_cast<int*>(body->GetUserData());
  world->DestroyBody(body);
}

void createDistanceJoint(int world_id, int body_a_id, int body_b_id) {
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

void setContinuous(int world_id, bool continuous) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  world->SetContinuousPhysics(continuous);
}

void setGravity(int world_id, double x, double y) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  b2Vec2 gravity;
  gravity.x = x;
  gravity.y = y;
  world->SetGravity(gravity);
  return;
}

void step(int world_id, double dt,
          int velocity_iterations, int position_iterations) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  world->Step(dt, velocity_iterations, position_iterations);
  b2Body* next_body = world->GetBodyList();
  next_body = world->GetBodyList();
  // TODO: create java arraylist
  while(next_body) {
    int body_id =
        *(reinterpret_cast<int*>(next_body->GetUserData()));
    float32 x = next_body->GetPosition().x;
    float32 y = next_body->GetPosition().y;
    float32 angle = next_body->GetAngle();
    // TODO: call java arraylist add (id,x,y,angle)
    next_body = next_body->GetNext();
  }

  return; // TODO: return jobject for arraylist and change the function type
}

void getLastContacts(int world_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  b2Contact* next_contact = world->GetContactList();
  // TODO: create java arraylist
  while(next_contact) {
    b2Fixture * fa = next_contact->GetFixtureA();
    b2Fixture * fb = next_contact->GetFixtureB();
    int fa_id = *(reinterpret_cast<int*>(fa->GetUserData()));
    int fb_id = *(reinterpret_cast<int*>(fb->GetUserData()));
    bool is_touching = next_contact->IsTouching();
    // TODO: call java arraylist add (fa_id,fb_id,is_touching)
    next_contact = next_contact->GetNext();
  }

  return; // TODO: return jobject for arraylist and change the function type
}

void clearForces(int world_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  world->ClearForces();
}

void setSensor(int world_id, int fixture_id, bool is_sensor) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  b2Fixture* fixture = getFixtureById(fixture_id);
  if (!fixture)
    return;
  fixture->SetSensor(is_sensor);
}

void setDensity(int world_id, int fixture_id, float density) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  b2Fixture* fixture = getFixtureById(fixture_id);
  if (!fixture)
    return;
  fixture->SetDensity(density);
}

void setFriction(int world_id, int fixture_id, float friction) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  b2Fixture* fixture = getFixtureById(fixture_id);
  if (!fixture)
    return;
  fixture->SetFriction(friction);
}

void setRestitution(int world_id, int fixture_id, float restitution) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;
  b2Fixture* fixture = getFixtureById(fixture_id);
  if (!fixture)
    return;
  fixture->SetRestitution(restitution);
}

int createBody(int world_id, int type, double x, double y) {
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

int createFixture(int world_id, int body_id,
                   double friction, double restitution, double density,
                   std::string type, double param1, double param2) {
  b2Shape* shape = NULL;
  if (type == std::string("circle")) {
    double radius = param1;
    shape = new b2CircleShape();
    shape->m_radius = radius;
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

void setBodyTransform(int world_id, int body_id, double x, double y, double angle) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  b2Vec2 position(x, y);
  body->SetTransform(position, angle);
}

void getLinearVelocity(int world_id, int body_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  b2Vec2 v = body->GetLinearVelocity();

  return; // TODO: return jobject with v.x and v.y, and change function type
}

void getWorldCenter(int world_id, int body_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  b2Vec2 p = body->GetWorldCenter();

  return; // TODO: return jobject with p.x and p.y, and change function type
}

void getLocalCenter(int world_id, int body_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  b2Vec2 p = body->GetLocalCenter();

  return; // TODO: return jobject with p.x and p.y, and change function type
}

void applyImpulse(int world_id, int body_id,
                  double impluse_x, double impluse_y,
                  double point_x, double point_y, bool wake) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  b2Vec2 impluse(impluse_x, impluse_y);
  b2Vec2 point(point_x, point_y);
  body->ApplyLinearImpulse(impluse, point, wake);
}

bool isAwake(int world_id, int body_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return false;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return false;

  bool wake = body->IsAwake();
  return wake;
}

float getAngularVelocity(int world_id, int body_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return 0;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return 0;

  float v = body->GetAngularVelocity();
  return v;
}

void setAwake(int world_id, int body_id, bool wake) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  body->SetAwake(wake);
}

void setLinearVelocity(int world_id, int body_id, double x, double y) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  b2Vec2 v(x, y);
  body->SetLinearVelocity(v);
}

void applyForceToCenter(int world_id, int body_id, double x, double y, bool wake) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  b2Vec2 force(x, y);
  body->ApplyForceToCenter(force, wake);
}

void setLinearDamping(int world_id, int body_id, double damp) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  body->SetLinearDamping(damp);
}

void setAngularVelocity(int world_id, int body_id, double w) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  body->SetAngularVelocity(w);
}

void setActive(int world_id, int body_id, bool active) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;

  body->SetActive(active);
}

void getObjectContacts(int world_id, int body_id) {
  b2World* world = getWorldById(world_id);
  if (!world)
    return;

  b2Body* body = getBodyById(body_id);
  if (!body)
    return;
  
  b2ContactEdge* next_edge = body->GetContactList();
  // TODO: create java arraylist
  while(next_edge) {
    b2Body* other = next_edge->other;
    int other_id =
        *(reinterpret_cast<int*>(other->GetUserData()));
    // TODO: call java arraylist add with other_id
    next_edge = next_edge->next;
  }

  return; // TODO: return jobject for arraylist and change function type
}
