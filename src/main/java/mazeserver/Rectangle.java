/**
 * Square.java
 *
 * Copyright 2016 Finn Bear.  All rights reserved.
 */
package mazeserver;

import com.google.gson.annotations.SerializedName;


public class Rectangle extends PhysicsObject {

    @SerializedName("width") float _width;
    @SerializedName("height") float _height;


    public Rectangle(String id, Vector2 position, float width, float height, float elasticity, float inertialDamper, float gravity)
    {
        super(id, position, Vector2.ZERO(), Vector2.ZERO(), elasticity, inertialDamper, gravity);
        _width = width;
        _height = height;
    }

    public Rectangle(String id, Vector2 position, Vector2 velocity, float width, float height, float elasticity, float inertialDamper, float gravity)
    {
        super(id, position, velocity, Vector2.ZERO(), elasticity, inertialDamper, gravity);
        _width = width;
        _height = height;
    }

    public Rectangle(String id, Vector2 position, Vector2 velocity, Vector2 acceleration, float width, float height, float elasticity, float inertialDamper, float gravity)
    {
        super(id, position, velocity, acceleration, elasticity, inertialDamper, gravity);
        _width = width;
        _height = height;
    }

    public Rectangle(String id, Square square)
    {
        super(id, square.getPosition(), square.getVelocity(), square.getAcceleration(), square.getElasticity(), square.getInertialDamper(), square.getGravity());
        _width = square.getSide();
        _height = square.getSide();
    }

    public float getWidth()
    {
        return _width;
    }

    public float setWidth(float width)
    {
        _width = Math.max(0, width);
        return _width;
    }

    public float getHeight()
    {
        return _height;
    }

    public float setHeight(float height)
    {
        _height = Math.max(0, height);
        return _height;
    }
}
