package org.lwjglb.engine.graph;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.lwjglb.engine.items.GameItem;

public class Mesh {

    public static final int MAX_WEIGHTS = 4;

    protected final int vaoId;

    protected final List<Integer> vboIdList;

    private final int vertexCount;

    private Material material;

    private float boundingRadius;

    private List<Vector3f> vertices;

    private Vector3f min;
    private Vector3f max;


    public Mesh(float[] positions, float[] textCoords, float[] normals, int[] indices) {
        this(positions, textCoords, normals, indices, createEmptyIntArray(MAX_WEIGHTS * positions.length / 3, 0), createEmptyFloatArray(MAX_WEIGHTS * positions.length / 3, 0));
    }

    public Mesh(float[] positions, float[] textCoords, float[] normals, int[] indices, int[] jointIndices, float[] weights) {
        FloatBuffer posBuffer = null;
        FloatBuffer textCoordsBuffer = null;
        FloatBuffer vecNormalsBuffer = null;
        FloatBuffer weightsBuffer = null;
        IntBuffer jointIndicesBuffer = null;
        IntBuffer indicesBuffer = null;

        List<Vector3f> vertices = new LinkedList<>();

        min = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        max = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);

        for(int i = 0; i < positions.length; i= i + 3) {
            float x = positions[i];
            float y = positions[i + 1];
            float z = positions[i + 2];
            vertices.add(new Vector3f(x, y, z));

            min.set(Float.min(min.x, x), Float.min(min.y, y), Float.min(min.z, z));
            max.set(Float.max(max.x, x), Float.max(max.y, y), Float.max(max.z, z));

        }

        try {
            calculateBoundingRadius(positions);
            
            vertexCount = indices.length;
            vboIdList = new ArrayList<>();

            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            // Position VBO
            int vboId = glGenBuffers();
            vboIdList.add(vboId);
            posBuffer = MemoryUtil.memAllocFloat(positions.length);
            posBuffer.put(positions).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

            // Texture coordinates VBO
            vboId = glGenBuffers();
            vboIdList.add(vboId);
            textCoordsBuffer = MemoryUtil.memAllocFloat(textCoords.length);
			if ( textCoordsBuffer.capacity() > 0 ) {
                textCoordsBuffer.put(textCoords).flip();
			} else {
				// Create empty structure. Two coordinates for each 3 position coordinates
				textCoordsBuffer = MemoryUtil.memAllocFloat((positions.length * 3) / 2);
			}
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, textCoordsBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);

            // Vertex normals VBO
            vboId = glGenBuffers();
            vboIdList.add(vboId);
            vecNormalsBuffer = MemoryUtil.memAllocFloat(normals.length);
			if ( vecNormalsBuffer.capacity() > 0 ) {
                vecNormalsBuffer.put(normals).flip();
			} else {
				vecNormalsBuffer = MemoryUtil.memAllocFloat(positions.length);
			}
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, vecNormalsBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);

            // Weights
            vboId = glGenBuffers();
            vboIdList.add(vboId);
            weightsBuffer = MemoryUtil.memAllocFloat(weights.length);
            weightsBuffer.put(weights).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, weightsBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(3, 4, GL_FLOAT, false, 0, 0);

            // Joint indices
            vboId = glGenBuffers();
            vboIdList.add(vboId);
            jointIndicesBuffer = MemoryUtil.memAllocInt(jointIndices.length);
            jointIndicesBuffer.put(jointIndices).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, jointIndicesBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(4, 4, GL_FLOAT, false, 0, 0);

            // Index VBO
            vboId = glGenBuffers();
            vboIdList.add(vboId);
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
        } finally {
            if (posBuffer != null) {
                MemoryUtil.memFree(posBuffer);
            }
            if (textCoordsBuffer != null) {
                MemoryUtil.memFree(textCoordsBuffer);
            }
            if (vecNormalsBuffer != null) {
                MemoryUtil.memFree(vecNormalsBuffer);
            }
            if (weightsBuffer != null) {
                MemoryUtil.memFree(weightsBuffer);
            }
            if (jointIndicesBuffer != null) {
                MemoryUtil.memFree(jointIndicesBuffer);
            }
            if (indicesBuffer != null) {
                MemoryUtil.memFree(indicesBuffer);
            }
        }
    }
    
    private void calculateBoundingRadius(float positions[]) {
        int length = positions.length;
        boundingRadius = 0;
        for(int i=0; i< length; i++) {
            float pos = positions[i];
            boundingRadius = Math.max(Math.abs(pos), boundingRadius);
        }
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public final int getVaoId() {
        return vaoId;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public float getBoundingRadius() {
        return boundingRadius;
    }

    public void setBoundingRadius(float boundingRadius) {
        this.boundingRadius = boundingRadius;
    }

    protected void initRender() {
        Texture texture = material != null ? material.getTexture() : null;
        if (texture != null) {
            // Activate first texture bank
            glActiveTexture(GL_TEXTURE0);
            // Bind the texture
            glBindTexture(GL_TEXTURE_2D, texture.getId());
        }
        Texture normalMap = material != null ? material.getNormalMap() : null;
        if (normalMap != null) {
            // Activate second texture bank
            glActiveTexture(GL_TEXTURE1);
            // Bind the texture
            glBindTexture(GL_TEXTURE_2D, normalMap.getId());
        }

        // Draw the mesh
        glBindVertexArray(getVaoId());
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glEnableVertexAttribArray(3);
        glEnableVertexAttribArray(4);
    }

    protected void endRender() {
        // Restore state
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glDisableVertexAttribArray(3);
        glDisableVertexAttribArray(4);
        glBindVertexArray(0);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void render() {
        initRender();

        glDrawElements(GL_TRIANGLES, getVertexCount(), GL_UNSIGNED_INT, 0);

        endRender();
    }

    public void renderList(List<GameItem> gameItems, Consumer<GameItem> consumer) {
        initRender();

        for (GameItem gameItem : gameItems) {
            if (gameItem.isInsideFrustum()) {
                // Set up data requiered by gameItem
                consumer.accept(gameItem);
                // Render this game item
                glDrawElements(GL_TRIANGLES, getVertexCount(), GL_UNSIGNED_INT, 0);
            }
        }

        endRender();
    }

    public void cleanUp() {
        glDisableVertexAttribArray(0);

        // Delete the VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        for (int vboId : vboIdList) {
            glDeleteBuffers(vboId);
        }

        // Delete the texture
        Texture texture = material.getTexture();
        if (texture != null) {
            texture.cleanup();
        }

        // Delete the VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }

    public void deleteBuffers() {
        // Delete the VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        for (int vboId : vboIdList) {
            glDeleteBuffers(vboId);
        }

        // Delete the VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }

    protected static float[] createEmptyFloatArray(int length, float defaultValue) {
        float[] result = new float[length];
        Arrays.fill(result, defaultValue);
        return result;
    }

    protected static int[] createEmptyIntArray(int length, int defaultValue) {
        int[] result = new int[length];
        Arrays.fill(result, defaultValue);
        return result;
    }

    public List<Vector3f> getVertices() {
        return vertices;
    }

    public Vector3f getMin() {
        return min;
    }

    public Vector3f getMax() {
        return max;
    }
}
