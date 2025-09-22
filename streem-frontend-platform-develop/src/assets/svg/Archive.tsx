import React from 'react';

function Archive(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg width="1em" height="1em" fill="none" viewBox="0 0 16 16" {...props}>
      <path fill="#000" d="M9 9.5H7v1h2v-1z" />
      <path
        fill="#000"
        d="M3 1v13a.997.997 0 001 1h8a.997.997 0 001-1V1H3zm9 13H4V8h8v6zm0-7H4V5h8v2zM4 4V2h8v2H4z"
      />
    </svg>
  );
}
export default Archive;
