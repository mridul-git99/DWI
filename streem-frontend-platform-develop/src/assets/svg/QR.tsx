import * as React from 'react';
import { SVGProps } from 'react';

const QR = (props: SVGProps<SVGSVGElement>) => (
  <svg width={32} height={32} fill="none" xmlns="http://www.w3.org/2000/svg" {...props}>
    <g clipPath="url(#a)" fill="#000">
      <path d="M22 24H10a2 2 0 0 1-2-2v-3h2v3h12v-3h2v3a2 2 0 0 1-2 2zm8-9H2v2h28v-2zm-6-2h-2v-3H10v3H8v-3a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v3zm6-3h-2V4h-6V2h8v8zM4 10H2V2h8v2H4v6zm6 20H2v-8h2v6h6v2zm20 0h-8v-2h6v-6h2v8z" />
    </g>
    <defs>
      <clipPath id="a">
        <path fill="#fff" d="M0 0h32v32H0z" />
      </clipPath>
    </defs>
  </svg>
);

export default QR;
