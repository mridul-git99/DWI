import React from 'react';

function PeerVerifiedIcon(props: React.SVGProps<SVGSVGElement>) {
  const color = props.color || '#42BE65';
  const size = props.size || 16;
  const fillColor = props.fill || 'white';
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 18 16"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect width="16" height="16" rx="8" fill={color} />
      <g clip-path="url(#clip0_882_35269)">
        <path
          d="M6.50033 12L2.66699 8.16667L3.37399 7.45967L6.50033 10.5855L13.2933 3.79301L14.0003 4.50001L6.50033 12Z"
          fill={fillColor}
        />
      </g>
      <path
        d="M9.66634 12.1035L5.83301 8.27015L6.54001 7.56315L9.66634 10.689L16.4593 3.89648L17.1663 4.60348L9.66634 12.1035Z"
        fill={fillColor}
      />
      <defs>
        <clipPath id="clip0_882_35269">
          <rect width="16" height="16" fill="white" />
        </clipPath>
      </defs>
    </svg>
  );
}

export default PeerVerifiedIcon;
