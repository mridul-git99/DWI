declare module '*.png' {
  const value: any;
  export default value;
}

declare module '*.ttf' {
  const ttf: any;
  export default ttf;
}

declare module '*.svg' {
  const svg: any;
  export default svg;
}

declare module '*.woff' {
  const woff: any;
  export default woff;
}

declare module '*.woff2' {
  const woff2: any;
  export default woff2;
}

declare module '*.eot' {
  const eot: string;
  export default eot;
}

declare interface Window {
  store: any;
  persistor: any;
}
