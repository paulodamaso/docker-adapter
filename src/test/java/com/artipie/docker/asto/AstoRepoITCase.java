/*
 * MIT License
 *
 * Copyright (c) 2020 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Remaining;
import com.artipie.asto.fs.FileStorage;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.ref.ManifestRef;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link AstoRepo}.
 * @since 0.1
 */
final class AstoRepoITCase {

    /**
     * Repository being tested.
     */
    private Repo repo;

    @BeforeEach
    void setUp() throws Exception {
        final Path dir = Path.of(
            Thread.currentThread().getContextClassLoader()
                .getResource("docker").toURI()
        ).getParent();
        this.repo = new AstoRepo(
            new FileStorage(dir, Vertx.vertx().fileSystem()),
            new RepoName.Simple("test")
        );
    }

    @Test
    void shouldReadManifest() throws Exception {
        final Optional<Content> manifest = this.repo.manifest(new ManifestRef(new Tag.Valid("1")))
            .toCompletableFuture()
            .get();
        final byte[] content = new Remaining(
            Flowable.fromPublisher(manifest.get())
                .toList()
                .blockingGet()
                .stream()
                .reduce(
                    (left, right) -> ByteBuffer.allocate(left.remaining() + right.remaining())
                        .put(left)
                        .put(right)
                ).orElse(ByteBuffer.allocate(0))
        ).bytes();
        // @checkstyle MagicNumberCheck (1 line)
        MatcherAssert.assertThat(content.length, Matchers.equalTo(942));
    }

    @Test
    void shouldReadNoManifestIfAbsent() throws Exception {
        final Optional<Content> manifest = this.repo.manifest(new ManifestRef(new Tag.Valid("2")))
            .toCompletableFuture()
            .get();
        MatcherAssert.assertThat(manifest.isPresent(), new IsEqual<>(false));
    }
}
