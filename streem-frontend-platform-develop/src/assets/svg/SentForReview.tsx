import * as React from 'react';
import { SVGProps } from 'react';

const SentForReview = (props: SVGProps<SVGSVGElement>) => (
  <svg xmlns="http://www.w3.org/2000/svg" width={250} height={159} fill="none" {...props}>
    <g clipPath="url(#a)" filter="url(#b)">
      <path fill="url(#c)" d="m58.9 80.7 8.9 45.3L246 3 58.9 80.7Z" />
      <path fill="url(#d)" d="M246 3 58.9 80.7 4 36.5 246 3Z" />
      <path fill="url(#e)" d="M80.7 98.3 67.8 126 246 3 80.7 98.3Z" />
      <path fill="url(#f)" d="M246 3 80.7 98.3l68.7 55.2L246 3Z" />
    </g>
    <defs>
      <linearGradient
        id="c"
        x1={58.381}
        x2={248.201}
        y1={68.231}
        y2={61.032}
        gradientUnits="userSpaceOnUse"
      >
        <stop stopColor="#7EB9FF" />
        <stop offset={1} stopColor="#ADD3FF" />
      </linearGradient>
      <linearGradient
        id="d"
        x1={4.416}
        x2={247.338}
        y1={47.488}
        y2={38.275}
        gradientUnits="userSpaceOnUse"
      >
        <stop stopColor="#BBDAFF" />
        <stop offset={1} stopColor="#E8F3FF" />
      </linearGradient>
      <linearGradient
        id="e"
        x1={65.603}
        x2={248.201}
        y1={67.957}
        y2={61.032}
        gradientUnits="userSpaceOnUse"
      >
        <stop stopColor="#5CA6FF" />
        <stop offset={1} stopColor="#79B6FF" />
      </linearGradient>
      <linearGradient
        id="f"
        x1={80.079}
        x2={248.781}
        y1={82.717}
        y2={76.319}
        gradientUnits="userSpaceOnUse"
      >
        <stop stopColor="#BBDAFF" />
        <stop offset={1} stopColor="#E8F3FF" />
      </linearGradient>
      <clipPath id="a">
        <path fill="#fff" d="M4 3h242v150.5H4z" />
      </clipPath>
      <filter
        id="b"
        width={250}
        height={158.5}
        x={0}
        y={0}
        colorInterpolationFilters="sRGB"
        filterUnits="userSpaceOnUse"
      >
        <feFlood floodOpacity={0} result="BackgroundImageFix" />
        <feColorMatrix in="SourceAlpha" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" />
        <feOffset dy={1} />
        <feGaussianBlur stdDeviation={2} />
        <feColorMatrix values="0 0 0 0 0.4 0 0 0 0 0.4 0 0 0 0 0.4 0 0 0 0.08 0" />
        <feBlend in2="BackgroundImageFix" result="effect1_dropShadow" />
        <feBlend in="SourceGraphic" in2="effect1_dropShadow" result="shape" />
      </filter>
    </defs>
  </svg>
);
export default SentForReview;
