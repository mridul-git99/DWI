import React from 'react';

function ViewInfo(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg width="1em" height="1em" fill="none" viewBox="0 0 16 16" {...props}>
      <path
        fill="#000"
        d="M8.5 11V7h-2v1h1v3H6v1h4v-1H8.5zM8 4a.751.751 0 10.002 1.5A.751.751 0 008 4z"
      />
      <path
        fill="#000"
        d="M13 14H3a1.002 1.002 0 01-1-1V3a1.002 1.002 0 011-1h10a1.002 1.002 0 011 1v10a1.002 1.002 0 01-1 1zM3 3v10h10V3H3z"
      />
    </svg>
  );
}

export default ViewInfo;
