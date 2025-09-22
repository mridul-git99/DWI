import React from 'react';

function SelfVerifiedIcon(props: React.SVGProps<SVGSVGElement>) {
  const color = props.color || '#42BE65';
  const size = props.size || 16;
  const fillColor = props.fill || 'white';
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 16 16"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect width="16" height="16" rx="8" fill={color} />
      <g clip-path="url(#clip0_900_32959)">
        <path
          d="M6.49996 12L2.66663 8.16664L3.37363 7.45964L6.49996 10.5855L13.293 3.79297L14 4.49997L6.49996 12Z"
          fill={fillColor}
        />
      </g>
      <defs>
        <clipPath id="clip0_900_32959">
          <rect width="16" height="16" fill="white" />
        </clipPath>
      </defs>
    </svg>
  );
}

export default SelfVerifiedIcon;
