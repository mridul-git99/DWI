import * as React from 'react';
const SoloTaskLock = (props: React.SVGProps<SVGSVGElement>) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="21"
    height="20"
    viewBox="0 0 21 20"
    fill="none"
    {...props}
  >
    <g fill="currentColor" clipPath="url(#clip0)">
      <path d="M8.4 2.5a3.125 3.125 0 1 1 0 6.25 3.125 3.125 0 0 1 0-6.25Zm0-1.25a4.375 4.375 0 1 0 0 8.75 4.375 4.375 0 0 0 0-8.75ZM14.65 18.75H13.4v-3.125a3.124 3.124 0 0 0-3.125-3.125h-3.75A3.125 3.125 0 0 0 3.4 15.625v3.125H2.15v-3.125a4.375 4.375 0 0 1 4.375-4.375h3.75a4.375 4.375 0 0 1 4.375 4.375v3.125ZM20.275 8.125h-.625V6.25a1.875 1.875 0 1 0-3.75 0v1.875h-.625a.625.625 0 0 0-.625.625v3.75a.625.625 0 0 0 .625.625h5a.624.624 0 0 0 .625-.625V8.75a.625.625 0 0 0-.625-.625Zm-3.75-1.875a1.25 1.25 0 0 1 2.5 0v1.875h-2.5V6.25Zm3.75 6.25h-5V8.75h5v3.75Z" />
    </g>
    <defs>
      <clipPath id="clip0">
        <path fill="#fff" d="M.9 0h20v20H.9z" />
      </clipPath>
    </defs>
  </svg>
);
export default SoloTaskLock;
