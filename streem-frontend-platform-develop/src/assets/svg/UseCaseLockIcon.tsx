import React from 'react';

export const UseCaseLockIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg
    className="category-lock-icon"
    width="32"
    height="32"
    viewBox="0 0 32 32"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    {...props}
  >
    <g filter="url(#a)">
      <path
        d="M4 16C4 9.373 9.373 4 16 4s12 5.373 12 12-5.373 12-12 12S4 22.627 4 16z"
        fill="#fff"
      />
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        d="M20 15h-1v-3a3 3 0 1 0-6 0v3h-1a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1v-6a1 1 0 0 0-1-1zm-6-3a2 2 0 1 1 4 0v3h-4v-3zm6 10h-8v-6h8v6z"
        fill="#1D84FF"
      />
    </g>
    <defs>
      <filter
        id="a"
        x="0"
        y="0"
        width="32"
        height="32"
        filterUnits="userSpaceOnUse"
        color-interpolation-filters="sRGB"
      >
        <feFlood flood-opacity="0" result="BackgroundImageFix" />
        <feColorMatrix
          in="SourceAlpha"
          values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0"
          result="hardAlpha"
        />
        <feOffset />
        <feGaussianBlur stdDeviation="2" />
        <feColorMatrix values="0 0 0 0 0.113725 0 0 0 0 0.517647 0 0 0 0 1 0 0 0 0.32 0" />
        <feBlend in2="BackgroundImageFix" result="effect1_dropShadow_127_6002" />
        <feBlend in="SourceGraphic" in2="effect1_dropShadow_127_6002" result="shape" />
      </filter>
    </defs>
  </svg>
);
