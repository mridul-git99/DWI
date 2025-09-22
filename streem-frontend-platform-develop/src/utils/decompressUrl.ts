export const decompressUrl = (urlEncodedString: string) => {
  try {
    const binaryString = atob(urlEncodedString);

    const len = binaryString.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }

    const cs = new DecompressionStream('gzip');
    const writer = cs.writable.getWriter();

    writer.write(bytes);
    writer.close();

    return new Response(cs.readable).arrayBuffer().then((arrayBuffer) => {
      return new TextDecoder().decode(arrayBuffer);
    });
  } catch (error) {
    console.error('Failed to decode or decompress state:', error);
    return {};
  }
};

// Utility to compress and base64 encode a JS object for SSO with query Param
export const compressState = async (stateObj: object) => {
  const jsonString = JSON.stringify(stateObj);
  const textEncoder = new TextEncoder();
  const encoded = textEncoder.encode(jsonString);

  const cs = new CompressionStream('gzip');
  const writer = cs.writable.getWriter();
  writer.write(encoded);
  writer.close();

  const compressedBuffer = await new Response(cs.readable).arrayBuffer();

  // Convert compressed buffer to Base64
  let binary = '';
  const bytes = new Uint8Array(compressedBuffer);
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }

  return btoa(binary);
};
