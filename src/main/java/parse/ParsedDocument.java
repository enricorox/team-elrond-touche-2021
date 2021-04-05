/*
 *  Copyright 2021 University of Padua, Italy
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package parse;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.lucene.document.Field;

/**
 * Represents a parsed document to be indexed.
 *
 * @author Nicola Ferro (ferro@dei.unipd.it)
 * @version 1.00
 * @since 1.00
 */
public class ParsedDocument {

    /**
     * The names of the {@link Field}s within the index.
     *
     * @author Nicola Ferro
     * @version 1.00
     * @since 1.00
     */
    public final static class FIELDS {

        /**
         * The document identifier
         */
        public static final String ID = "id";

        /**
         * The document identifier
         */
        public static final String BODY = "body";

        public static final String DOMAIN = "domain";
    }


    /**
     * The unique document identifier.
     */
    private final String id;

    /**
     * The body of the document.
     */
    private final String body;

    private final String domain;

    /**
     * Creates a new parsed document
     *
     * @param id   the unique document identifier.
     * @param body the body of the document.
     * @throws NullPointerException  if {@code id} and/or {@code body} are {@code null}.
     * @throws IllegalStateException if {@code id} and/or {@code body} are empty.
     */
    public ParsedDocument(final String id, final String body, final String domain) {

        if (id == null) {
            throw new NullPointerException("Document identifier cannot be null.");
        }

        if (id.isEmpty()) {
            throw new IllegalStateException("Document identifier cannot be empty.");
        }

        this.id = id;

        if (body == null) {
            throw new NullPointerException("Document body cannot be null.");
        }

        if (body.isEmpty()) {
            throw new IllegalStateException("Document body cannot be empty.");
        }

        this.body = body;

        if (domain == null) {
            throw new NullPointerException("Document domain cannot be null.");
        }

        if (domain.isEmpty()) {
            throw new IllegalStateException("Document domain cannot be empty.");
        }

        this.domain = domain;
    }

    /**
     * Returns the unique document identifier.
     *
     * @return the unique document identifier.
     */
    public String getIdentifier() {
        return id;
    }

    /**
     * Returns the body of the document.
     *
     * @return the body of the document.
     */
    public String getBody() {
        return body;
    }

    public String getDomain() {
        return domain;
    }

    @Override
    public final String toString() {
        ToStringBuilder tsb = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("identifier", id).append(
                "body", body).append("domain", domain);

        return tsb.toString();
    }

    @Override
    public final boolean equals(Object o) {
        return (this == o) || ((o instanceof ParsedDocument) && id.equals(((ParsedDocument) o).id));
    }

    @Override
    public final int hashCode() {
        return 37 * id.hashCode();
    }


}
