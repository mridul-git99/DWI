import React from 'react';

function OntologyIcon(props: React.SVGProps<SVGSVGElement>) {
  const color = props.color || '#161616';
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="none" viewBox="0 0 18 18">
      <path
        fill={color}
        fillRule="evenodd"
        d="M15.875 12.838V10.25c0-.69-.56-1.25-1.25-1.25h-5V6.5h1.25c.69 0 1.25-.56 1.25-1.25V1.5c0-.69-.56-1.25-1.25-1.25h-3.75c-.69 0-1.25.56-1.25 1.25v3.75c0 .69.56 1.25 1.25 1.25h1.25V9h-5c-.69 0-1.25.56-1.25 1.25v2.589a2.5 2.5 0 101.25 0V10.25h5v2.589a2.5 2.5 0 101.25 0V10.25h5v2.588a2.5 2.5 0 101.25 0zM7.125 1.5h3.75v3.75h-3.75V1.5zM4 15.25a1.25 1.25 0 11-2.5 0 1.25 1.25 0 012.5 0zm6.25 0a1.25 1.25 0 11-2.5 0 1.25 1.25 0 012.5 0zm5 1.25a1.25 1.25 0 110-2.5 1.25 1.25 0 010 2.5z"
        clipRule="evenodd"
      ></path>
    </svg>
  );
}

export default OntologyIcon;
