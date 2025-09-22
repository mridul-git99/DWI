import React, { forwardRef, FC, useRef, useEffect, useCallback, useMemo } from 'react';
import {
  BeautifulMentionsPlugin,
  BeautifulMentionsMenuProps,
  BeautifulMentionsMenuItemProps,
  BeautifulMentionComponentProps,
  createBeautifulMentionNode,
  BeautifulMentionsMenuItem,
} from 'lexical-beautiful-mentions';
import { InitialConfigType, LexicalComposer } from '@lexical/react/LexicalComposer';
import { RichTextPlugin } from '@lexical/react/LexicalRichTextPlugin';
import { ContentEditable } from '@lexical/react/LexicalContentEditable';
import { OnChangePlugin } from '@lexical/react/LexicalOnChangePlugin';
import styled from 'styled-components';
import LexicalErrorBoundary from '@lexical/react/LexicalErrorBoundary';
import { EditorState, LexicalEditor } from 'lexical';
import { TextInput } from './Input';
import CancelIcon from '@material-ui/icons/Cancel';
import CheckCircleIcon from '@material-ui/icons/CheckCircle';
import { Popover } from '@material-ui/core';
import { EditorRefPlugin } from '@lexical/react/LexicalEditorRefPlugin';
import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext';
import { v4 as uuidv4 } from 'uuid';

const Wrapper = styled.div<{
  isMultiLine?: boolean;
}>`
  .editor-content {
    border: 1px solid #ccc;
    padding: 10px;
    background-color: #fff;
    height: ${(props) => (props.isMultiLine ? '150px' : 'unset')};
    overflow-y: scroll;

    p {
      margin: 0;
    }
  }

  .editor-placeholder {
    font-size: 12px;
    color: #525252;
    margin-top: 4px;
  }

  .editor-label {
    font-size: 12px;
    margin-bottom: 8px;
    color: #525252;
  }
`;

const PopoverWrapper = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
`;

type TMentionProps = {
  label: string;
  mentionItems: any;
  onChange: (value: any) => void;
  placeholder?: string;
  isMultiLine?: boolean;
  initialState?: any;
  isReadOnly: boolean;
};

const CustomMentionComponent = forwardRef<HTMLDivElement, BeautifulMentionComponentProps<any>>(
  ({ trigger, value, data, children, ...other }, ref) => {
    const [anchorEl, setAnchorEl] = React.useState<HTMLButtonElement | null>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    const [editor] = useLexicalComposerContext();

    const isEditable = editor.isEditable();

    const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
      setAnchorEl(event.currentTarget);
    };

    const handleClose: React.MouseEventHandler<SVGSVGElement> = (e) => {
      e?.stopPropagation();
      setAnchorEl(null);
    };

    const updatePostfixData = (json: any, uuid: string, newValue: string) => {
      const updateRecursive = (node: any) => {
        if (node?.data?.uuid === uuid) {
          node.data['postfix'] = newValue;
          return true;
        }
        if (node?.children) {
          return node.children.some((child: any) => updateRecursive(child));
        }
        return false;
      };
      updateRecursive(json.root);
    };

    const onSubmit: React.MouseEventHandler<SVGSVGElement> = (e) => {
      e?.stopPropagation();
      if (inputRef.current) {
        const value = inputRef.current.value;
        const json = editor?.getEditorState().toJSON();

        if (json) {
          updatePostfixData(json, data?.uuid, value);

          const editorState = editor.parseEditorState(JSON.stringify(json));
          editor.setEditorState(editorState);
        }
      }
    };

    return (
      <div
        {...other}
        ref={ref}
        onClick={(e) => {
          handleClick(e);
        }}
        style={{
          display: 'inline',
        }}
      >
        <span
          title={trigger + value}
          style={{
            padding: '2px 4px',
            backgroundColor: '#16A34A',
            borderRadius: '4px',
            color: '#fff',
            fontSize: '12px',
            cursor: 'pointer',
          }}
        >
          {value}
        </span>
        <Popover
          id={'mentions-popover'}
          open={!!anchorEl}
          anchorEl={anchorEl}
          onClose={handleClose}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'left',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'left',
          }}
          style={{
            marginTop: '8px',
          }}
        >
          <PopoverWrapper>
            <TextInput
              defaultValue={data?.postfix}
              onChange={({ value }: any) => {}}
              placeholder="Enter details"
              ref={inputRef}
              disabled={!isEditable}
            />
            {isEditable && (
              <>
                <CheckCircleIcon onClick={onSubmit} style={{ cursor: 'pointer' }} />
                <CancelIcon onClick={handleClose} style={{ cursor: 'pointer' }} />
              </>
            )}
          </PopoverWrapper>
        </Popover>
      </div>
    );
  },
);

function CustomMenu({ loading, ...props }: BeautifulMentionsMenuProps) {
  return (
    <ul
      {...props}
      style={{
        listStyle: 'none',
        backgroundColor: 'white',
        border: '1px solid #ccc',
        borderRadius: '4px',
        boxShadow: '0 0 5px rgba(0, 0, 0, 0.1)',
        position: 'absolute',
        zIndex: 1500,
        padding: '0',
        margin: '0',
        width: 'max-content',
        maxHeight: 200,
        overflowY: 'scroll',
      }}
    />
  );
}

const CustomMenuItem = forwardRef<HTMLLIElement, BeautifulMentionsMenuItemProps>(
  ({ selected, item, ...props }, ref) => (
    <li
      style={{
        padding: '8px',
        cursor: 'pointer',
        borderBottom: '1px solid #ccc',
        fontSize: '14px',
      }}
      {...props}
      ref={ref}
    />
  ),
);

const Mentions: FC<TMentionProps> = ({
  label,
  mentionItems,
  placeholder,
  isMultiLine = false,
  onChange,
  initialState,
  isReadOnly,
}) => {
  const editor = useRef<LexicalEditor>(null);

  const editorConfig: InitialConfigType = {
    theme: {},
    namespace: 'MyEditor',
    nodes: [...createBeautifulMentionNode(CustomMentionComponent)],
    onError: (error) => {
      console.error('Lexical Editor Error:', error);
    },
    editable: !isReadOnly,
  };

  const handleChange = useCallback((editorState: EditorState) => {
    onChange(JSON.stringify(editorState.toJSON()));
  }, []);

  const menuItemsMaxLength = useMemo(() => {
    let maxLength = 0;

    for (const key in mentionItems) {
      if (Array.isArray(mentionItems[key])) {
        maxLength = Math.max(maxLength, mentionItems[key].length);
      }
    }

    return maxLength;
  }, [mentionItems]);

  useEffect(() => {
    if (editor.current && initialState) {
      const parsedState = editor.current.parseEditorState(initialState);
      editor.current.setEditorState(parsedState);
    }
  }, [editor.current]);

  return (
    <Wrapper isMultiLine={isMultiLine}>
      <div className="editor-label">{label}</div>
      <LexicalComposer initialConfig={editorConfig}>
        <EditorRefPlugin editorRef={editor} />
        <RichTextPlugin
          contentEditable={<ContentEditable className="editor-content" />}
          ErrorBoundary={LexicalErrorBoundary}
        />
        <BeautifulMentionsPlugin
          items={mentionItems}
          menuComponent={CustomMenu}
          menuItemComponent={CustomMenuItem}
          menuItemLimit={menuItemsMaxLength}
          preTriggerChars="."
          onMenuItemSelect={(mention: BeautifulMentionsMenuItem) => {
            if (mention && mention.data) {
              mention.data.uuid = uuidv4();
            }
          }}
        />
        <OnChangePlugin onChange={handleChange} />
      </LexicalComposer>
      {placeholder && <div className="editor-placeholder">{placeholder}</div>}
    </Wrapper>
  );
};

export default Mentions;
