import React from 'react';

const AvatarIcon = ({ className }: { className?: string }) => (
  <svg
    width="20"
    height="20"
    viewBox="0 0 20 20"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={className}
  >
    <path
      fillRule="evenodd"
      clipRule="evenodd"
      d="M1.25 10a8.75 8.75 0 1 1 17.5 0 8.75 8.75 0 0 1-17.5 0zm8.745-5.631a3.125 3.125 0 1 0 0 6.25 3.125 3.125 0 0 0 0-6.25zm0 5a1.875 1.875 0 1 1 0-3.75 1.875 1.875 0 0 1 0 3.75zM6.25 16.488v-1.25a2.012 2.012 0 0 1 1.875-2.113h3.75a2.012 2.012 0 0 1 1.875 2.119v1.25a7.45 7.45 0 0 1-7.5 0v-.006zM15 15.194v.368a7.5 7.5 0 1 0-10 0v-.368a3.25 3.25 0 0 1 3.125-3.319h3.75A3.256 3.256 0 0 1 15 15.194z"
      fill="#161616"
    />
  </svg>
);

export default AvatarIcon;
