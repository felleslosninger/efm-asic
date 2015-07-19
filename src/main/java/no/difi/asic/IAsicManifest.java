package no.difi.asic;

import java.security.MessageDigest;

/**
 * Defining a two-calls pattern. Fetch the message digest for calculation, then add
 * the metadata belonging to the message digest.
 */
interface IAsicManifest {

    /**
     * Fetch a reset message digest.
     *
     * @return Message digest ready for use.
     */
    MessageDigest getMessageDigest();

    /**
     * Metadata belong to the previous message digest calculation.
     *
     * @param filename Filename in container
     * @param mimeType Content type of content
     */
    void add(String filename, String mimeType);
}
