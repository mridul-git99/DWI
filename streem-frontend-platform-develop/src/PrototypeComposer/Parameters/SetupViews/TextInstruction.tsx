import { EmojiComponent } from '#components';
import { emojis } from '#utils/constants';
import { ContentState, convertToRaw, EditorState } from 'draft-js';
import draftToHtml from 'draftjs-to-html';
import htmlToDraft from 'html-to-draftjs';
import { debounce } from 'lodash';
import React, { FC, useEffect, useState } from 'react';
import { Editor } from 'react-draft-wysiwyg';
import 'react-draft-wysiwyg/dist/react-draft-wysiwyg.css';
import { useFormContext } from 'react-hook-form';
import styled from 'styled-components';

const TextInstructionWrapper = styled.div<{
  isReadOnly: boolean;
}>`
  padding-bottom: 16px;
  height: 100%;
  .wrapper-class {
    height: 100%;
  }

  .editor-class {
    overflow-wrap: break-word;
    border: 1px solid #bababa;
    padding: 0 16px;
    height: calc(100% - 30px);

    * {
      font-weight: unset;
    }

    :active {
      border-color: #1d84ff;
    }

    .public-* {
      margin: 0;
    }
  }

  .toolbar-class {
    align-items: center;
    background-color: #f4f4f4;
    display: ${({ isReadOnly }) => (isReadOnly ? 'none' : 'flex')};
    margin-bottom: 0;
    padding: 4px 8px;

    > div:nth-child(1n) {
      border-bottom: 0;
      border-right: 1px solid #666666;
      margin-bottom: 0;

      > div {
        background: transparent;
      }

      .rdw-emoji-modal {
        background: #ffffff;
        height: auto;
        width: 294px;
        padding: 10px;

        .rdw-emoji-icon {
          padding: 4px;
          margin: unset;
          height: 34px;
          width: 34px;
        }
      }

      :last-child {
        border-right: none;
        padding-right: 0;
      }
    }
  }
`;

const toolbarOptions = {
  options: ['inline', 'list', 'emoji'],
  inline: { options: ['bold', 'underline'] },
  list: { options: ['unordered', 'ordered'] },
  emoji: {
    emojis,
    component: EmojiComponent,
  },
};

const TextInstruction: FC<{ isReadOnly: boolean }> = ({ isReadOnly }) => {
  const [editorState, setEditorState] = useState<EditorState | null>(null);
  const { register, setValue, getValues } = useFormContext();
  register('data.text', {
    required: true,
    validate: (value) => {
      return value && /\S/.test(value.replace(/<[^>]*>|&nbsp;/g, '')); // removes html tags and nbsp;
    },
  });

  useEffect(() => {
    const text = getValues('data.text');
    const contentBlock = htmlToDraft(text || '');
    if (contentBlock) {
      const contentState = ContentState.createFromBlockArray(contentBlock.contentBlocks);
      setEditorState(EditorState.createWithContent(contentState));
    } else {
      setEditorState(EditorState.createEmpty());
    }
  }, []);

  if (!editorState) return null;

  return (
    <TextInstructionWrapper isReadOnly={isReadOnly}>
      <Editor
        defaultEditorState={editorState}
        wrapperClassName="wrapper-class"
        editorClassName={`editor-class`}
        toolbarClassName="toolbar-class"
        toolbar={toolbarOptions}
        onChange={debounce((value) => {
          value = draftToHtml(convertToRaw(editorState.getCurrentContent()));
          setValue('data.text', value, {
            shouldDirty: true,
            shouldValidate: true,
          });
        }, 500)}
        onEditorStateChange={(newEditorState) => setEditorState(newEditorState)}
        readOnly={isReadOnly}
      />
    </TextInstructionWrapper>
  );
};

export default TextInstruction;
