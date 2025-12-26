import { Editor } from '@monaco-editor/react';
import { useState, useEffect } from 'react';
import './CodeEditor.css';

function CodeEditor({ onSubmit, problemId, disabled, skeletonCode }) {
    const [code, setCode] = useState('');
    const [language, setLanguage] = useState('python');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [hasUserCode, setHasUserCode] = useState(false); // Track if user has modified code

    // Initialize code when problem loads or language changes
    useEffect(() => {
        if (!hasUserCode && skeletonCode && skeletonCode[language]) {
            // Use skeleton code from the problem, unescaping newlines
            setCode(unescapeNewlines(skeletonCode[language]));
        } else if (!hasUserCode) {
            // Fall back to generic starter code if no skeleton provided
            setCode(getStarterCode(language));
        }
    }, [language, skeletonCode, hasUserCode]);

    const handleSubmit = async () => {
        if (!code.trim()) {
            alert('Please write some code before submitting!');
            return;
        }

        setIsSubmitting(true);
        try {
            await onSubmit(code, language);
        } finally {
            setTimeout(() => setIsSubmitting(false), 2000);
        }
    };

    const handleLanguageChange = (newLang) => {
        // Only switch if user hasn't written code yet
        if (!hasUserCode || window.confirm('Switch language? Your current code will be replaced with starter code.')) {
            setLanguage(newLang);
            setHasUserCode(false); // Reset to allow new skeleton code
        }
    };

    const handleCodeChange = (value) => {
        setCode(value || '');
        if (value && value.trim()) {
            setHasUserCode(true); // User has started editing
        }
    };

    return (
        <div className="code-editor-container">
            <div className="editor-header">
                <div className="editor-controls">
                    <label htmlFor="language-select">Language:</label>
                    <select
                        id="language-select"
                        value={language}
                        onChange={(e) => handleLanguageChange(e.target.value)}
                        disabled={disabled || isSubmitting}
                        className="language-selector"
                    >
                        <option value="python">Python</option>
                        <option value="java">Java</option>
                        <option value="cpp">C++</option>
                        <option value="c">C</option>
                        <option value="javascript">JavaScript</option>
                    </select>

                    <button
                        onClick={handleSubmit}
                        disabled={disabled || isSubmitting}
                        className="btn-submit-code"
                    >
                        {isSubmitting ? (
                            <>
                                <span className="spinner"></span>
                                Running...
                            </>
                        ) : (
                            '▶ Submit Code'
                        )}
                    </button>
                </div>
            </div>

            <Editor
                height="500px"
                language={getMonacoLanguage(language)}
                value={code}
                onChange={handleCodeChange}
                theme="vs-dark"
                options={{
                    minimap: { enabled: false },
                    fontSize: 14,
                    scrollBeyondLastLine: false,
                    wordWrap: 'on',
                    automaticLayout: true,
                    tabSize: 4,
                }}
            />
        </div>
    );
}

// Utility function to handle escaped newlines from backend
const unescapeNewlines = (str) => {
    if (!str) return '';
    // Replace literal \n with actual newlines, and \t with tabs
    return str.replace(/\\n/g, '\n').replace(/\\t/g, '\t');
};

// Helper functions
function getMonacoLanguage(lang) {
    const mapping = {
        'cpp': 'cpp',
        'c': 'c',
        'java': 'java',
        'python': 'python',
        'javascript': 'javascript'
    };
    return mapping[lang] || 'python';
}

function getStarterCode(language) {
    const templates = {
        python: `# Read input
n = int(input())

# Your solution here
result = n

# Print output
print(result)`,

        java: `import java.util.Scanner;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        
        // Your solution here
        int result = n;
        
        System.out.println(result);
    }
}`,

        cpp: `#include <iostream>
using namespace std;

int main() {
    int n;
    cin >> n;
    
    // Your solution here
    int result = n;
    
    cout << result << endl;
    return 0;
}`,

        c: `#include <stdio.h>

int main() {
    int n;
    scanf("%d", &n);
    
    // Your solution here
    int result = n;
    
    printf("%d\\n", result);
    return 0;
}`,

        javascript: `// Read input
const readline = require('readline');
const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

rl.on('line', (line) => {
    const n = parseInt(line);
    
    // Your solution here
    const result = n;
    
    console.log(result);
    rl.close();
});`
    };

    return templates[language] || templates.python;
}

export default CodeEditor;
