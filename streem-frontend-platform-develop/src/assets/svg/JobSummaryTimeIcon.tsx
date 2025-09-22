import * as React from 'react';

function SummaryTimeIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" {...props}>
      <g clipPath="url(#prefix__clip0)" fill="#C29004">
        <path d="M19.27 7.144l1.437-1.437-1.414-1.414-1.537 1.537A9.937 9.937 0 0013 4.05V2h2V0H9v2h2v2.05a9.937 9.937 0 00-4.756 1.78L4.707 4.293 3.293 5.707 4.73 7.144a10 10 0 1014.54 0zM12 22a8 8 0 118-8 8.009 8.009 0 01-8 8z" />
        <path d="M12 8v6H6a6 6 0 106-6z" />
      </g>
      <defs>
        <clipPath id="prefix__clip0">
          <path fill="#fff" d="M0 0h24v24H0z" />
        </clipPath>
      </defs>
    </svg>
  );
}

const MemoSummaryTimeIcon = React.memo(SummaryTimeIcon);
export default MemoSummaryTimeIcon;
