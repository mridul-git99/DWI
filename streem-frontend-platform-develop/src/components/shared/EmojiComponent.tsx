import React from 'react';
import { Emoji } from '#types';

export const EmojiComponent = (props) => {
  const {
    config: { icon, title, popupClassName = '', emojis },
    expanded,
    onExpandEvent,
    translations,
    onChange,
  } = props;

  const selectEmoji = (event: React.MouseEvent<HTMLButtonElement, MouseEvent>): void => {
    const { target } = event;
    onChange((target as HTMLButtonElement).innerHTML);
  };

  const renderEmojiModal = () => {
    return (
      <div className={`rdw-emoji-modal ${popupClassName}`} onClick={(e) => e.stopPropagation()}>
        {emojis.map((Emoji: Emoji, index: number) => (
          <span key={index} className="rdw-emoji-icon" title={Emoji.name} onClick={selectEmoji}>
            {Emoji.value}
          </span>
        ))}
      </div>
    );
  };

  return (
    <div
      className="rdw-emoji-wrapper"
      aria-haspopup="true"
      aria-label="rdw-emoji-control"
      aria-expanded={expanded}
      title={title || translations['components.controls.emoji.emoji']}
    >
      <div className="rdw-option-wrapper" onClick={onExpandEvent}>
        <img src={icon} alt="" />
      </div>
      {expanded ? renderEmojiModal() : undefined}
    </div>
  );
};
