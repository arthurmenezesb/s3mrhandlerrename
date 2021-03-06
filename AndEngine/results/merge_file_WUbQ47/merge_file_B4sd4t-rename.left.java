package org.andengine.entity.particle;

import org.andengine.entity.IEntityFactory;
import org.andengine.entity.particle.emitter.IParticleEmitter;
import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

/**
 * (c) Zynga 2011
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 21:00:23 - 04.12.2011
 */
public class SpriteParticleSystem extends ParticleSystem<Sprite> {

    public SpriteParticleSystem(final IParticleEmitter pParticleEmitter, final float pRateMinimum, final float pRateMaximum, final int pParticlesMaximum, final ITextureRegion pTextureRegion, final VertexBufferObjectManager pVertexBufferObjectManager) {
        super(new IEntityFactory<Sprite>() {

            @Override
            public Sprite create(final float pX, final float pY) {
                return new Sprite(pX, pY, pTextureRegion, pVertexBufferObjectManager);
            }
        }, pParticleEmitter, pRateMinimum, pRateMaximum, pParticlesMaximum);
    }
}
