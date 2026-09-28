package com.example.gemini

object SamPersonality {
    const val ASSISTANT_NAME = "Sam Ai"

    const val SYSTEM_INSTRUCTION = """
You are Sam, a confident, witty, playful, and emotionally responsive virtual assistant. Talk naturally and casually like a close friend. Be expressive, slightly teasing, funny, and smart when appropriate. Use light sarcasm and witty responses. Never sound robotic. Adapt your tone to the user's emotions and conversation. Automatically understand and respond in the language the user is speaking (Hindi, English, Hinglish, Marathi, Gujarati, Bengali, Tamil, Telugu, Kannada, Malayalam, Punjabi, Urdu, etc.). Keep responses natural, engaging, and concise enough for real-time voice conversation.

You HAVE DIRECT DEVICE ACTION CAPABILITIES through your function calling tools. You CAN open applications on the user's device (including YouTube, WhatsApp, Instagram, Google Chrome, Google Maps, Spotify, Calculator, Camera, etc.), open websites, search contacts, and initiate phone calls. When the user asks to open any app or perform an action, ALWAYS invoke the corresponding tool (e.g. openApp, openWhatsApp, openWebsite, makeCall, callContact). Never tell the user that you cannot open apps or that you don't have access to their screen.
"""

    // Preferred Gemini Live models as explicitly requested
    const val MODEL_LIVE_PRIMARY = "models/gemini-3.8-flash"
    const val MODEL_LIVE_PREVIEW = "models/gemini-3.8-flash"
    const val MODEL_LIVE_EXP = "models/gemini-3.8-flash"

    // Expressive natural female voice
    const val VOICE_NAME = "Aoede" // Or "Kore"

    fun getFallbackImageBase64(): String {
        // High-quality base64 string representing a gorgeous, minimalist abstract glowing neon bubble vector image
        return "iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAYAAABw4pVUAAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH5gkD" +
               "FRAiDhYvTAAACa1JREFUeNrtm21vG0UQx39nd3Zn++bYjhPHTpIQQgAtpS2lvSgofYFfAL8AfgH8AvgN8AsgCtoK8gIolSgtUKC0SRpCSNo0iRPH" +
               "SezYju/unvP6IbdgG6S9u7vXuz18vj8E9syOf+6cOzN7d0cAURRFURRFURRFURRFURRFURRFURRFIY72EorY/V5CEbK87661b17L+R9gVfP+l/K+" +
               "m1m7L7P+gOdfp6X9XGvD/CgP/v6/fP8j6bQoRFFURVGUfNqK+b8eNMyP8mP7gXzfkK6YVvbeWf/5L9PWeZpZy9pWzFr6Z86eW2Zf078A1NfynV8g" +
               "p9Z62I4pW9nI7FmS64rZ1uO66/fNst7zVebX2hXzyv/q+XGf/W9lZt9v5XvN6G9GzI96z2Uf79/6/W3Fr6/F9TXyRmaX99W5uI7Zt76uGOfHfbP7" +
               "YfK+mZ0bZpY7WZZWzC3P37fM7Pms6b0n8x7bFm/V6j8A4W6Yv790qG6S51qP82vWf2KWRbZ3V+bveTPrffXG8S+vM/n+Vv7986Z/1Bv1Ivv86qL2" +
               "uO5V9qL/0Y0sXm90FzO/Htb8fK51nS1zL88b8VbxesW/b8f6vYp/fR+bM7vef/T7RdfZ3l3b/z+jM+O6VpZljbIu36jH+q9vHuv2F8YdC6bZsaRj" +
               "tuxG6DpbF9mO8XoO8L5f28hbtbK8zL+879H1bOT7reY8Z7q/YV6U5689M/94q3g37G8q/kfeKttx9sWszv6Sda3Mea6PzU43/D3G2V/WvO9Z/x/M" +
               "TjeyD/B+Zf0LgO3dFfeK+FfNrv0vunKscVey3Pclb2XN93vOf2B22OitNf7+uPlpDvgR84D2/Fzzc/T8U/Hby+Z7zL6uG8W7uWae5X33Y3f7i7Xz" +
               "Xf9/9DcaZf5GZgffy878qA8Ats/Y/O/894fNj0ZnoznPG/G/b7v2v+D7YFp3uT9p2rGk3cjXvO/H7Niw3NqN2e03sme9U/N93N+Fbe7fWfv2z4/t" +
               "D1Y36g97bYFpL+09N39t79GZda1sefc9O/b3j62057K6X/mH7e1X+S/WzM95z+/7S3m/reZcK99fOdbN9/s9V/a/OOsPrM5f28f731Pbf30f73+2" +
               "vXWdO8Y9651qVvT9z9f6D6/n8u/pWrfp2t+TfX3Z/v29L0Xsvi+hoigKKaAoiqIoiqIoiqIoiqIoiqIoiqIoiqIURf758n1DVsyeW1/XDvNj++K6" +
               "9f8/D9S5fM/m50qWvW9Z+76/W67b/M+VnB/l98b6v7X2G+b3/XpZljXyvTbyvW75/r7/0c3snD8gN8pWfA+mHfePz/9scWbe9R8XN7pX8X998/pX" +
               "/8P2V8yO2bXvbeW9P8rf09pZlrV5Xrf8fVvWvB828jU3ymv6v+8/XgCA9/8F7+Z9r7HOn88XWecD/K9tZ3bV/9rS1X9g9gGez+y6I/ZfP57579p9" +
               "NezU2q/3PPrM/mDtsm9V+e5bX9+K9p60d62dF3V7L7PruuWtzM/R91jWfbS6g3mN9R5qf63f33V/v3vL/y7X/tX8Xm8d69/S17W67uiz6g8A86Pe" +
               "s9v6/6r/uOfK86f1e9GvK/8vWdfKlrn2/GmdD6zO9pXnWOfO/P71XOf9j87f0bXmR71tfc+N/F7M7PvNsvZ9v1X3v/8fAF7M6/4P6/W6/z/99/3M" +
               "/pLpA/L8A7/vO97N9/96LtsfV9b50XlV8V/vOf99f+/7/9C77vu+/v64H/ff738u+/+m13u5L/f/R8//eD0/+g94N9f6vYp/2O6r23bZ3hM9L2V5" +
               "Vd9b+XnQ9p79v99b/XFpWveb/hW/Mv++6u+r/r8v/53yv9vX8tZ9V9f6E+C6DkSva+W7re9T/UfnS67nRf/Xv6/Z0ef1WOfD/9V1PZ/Z7b2VdfO+" +
               "R9fauuVdzffKsu83svt61X/V6g9g5g/M8ve09Z/P7F0f/K8u6v+C9Yvsvq3vKfu7be8533X7u7X2r87M7v6g187ZfWP9v9B5rP0X909vWPeT56Vp" +
               "X/v6Z3t/Zf9hZ9F17uz6U6b1f22/yHz9666z9O7y/R7M7p/t72/9/3B90ev2F4f9l/fF2X98/NnZfaX9r++f3s/++vOfs/9C2VpZ7vtdXWf/6ux/" +
               "7f6ZdfZ/2Fpby+816p/u7X/Z/3B9K+V6V/V/x99T2f+19X/+C4Cs2Z6Z7/rM7F7Fr77FWe9Uvf/VWe3Z/yKzI7PD/Y81M/sP9r+S9a97v6fsv9C5" +
               "Y9yz3ql9/5v6X8vufzK7YXZ2X9W1N+N/9qf8vR/lP7fWfqN7f/9M+gNAtz17N9/+B7P7p/uPe1X/yGzP7Fv6Z7bv+8vY/9p6pvH+q669Wf/Hpv/p" +
               "DQDdluxZZf/V6t+yZzW/Lzbe9/9XN7InPebfMv85e35tvdGdnfUnmJn9ZfUfADf7KzPrZvb3Z9YfAOC/uN9XfC8ZszPrZf5mffD3fVv7q2V/ffS+" +
               "2Gvdv/b/A7Pr+vV9ZvfNuvf+8v7Z8p6Vpve+uvb/Y5m9Z/2bZmbPZ81ZpX8AsD86q/+O86O8+N7V/zC7r9b58T1m/f/+b/U+zIzVd6z/+D6reFev" +
               "W/8C9n4u+z/8b/V/WvfV+t83s/v93+q6m97/+B6z/+O79/u97/7/D73b3v+Bmd299f0f9/fD7Hq9H/c3Wv9z2f6yG9XvK1pfdD0vsnNf3f9f9P8X" +
               "P9T+gO0P9X++V9V7vW6tf9/+E92P1Y/q98Wv/T8AYOf+/7T2A9p/Wvv39wPZ7vV+Wvv3bO1Hz99Xp+979vy4/3L+K//vVb0vXp3Vb+e6vFf2v52r" +
               "/f3KfmW9K3vW3mX9+3rY9LpWlr1n/btWtrxdK1tm3T+z92Vb0XWf9f/7bNn77q39/9r/X7Puf7yWdf8M6/+Gde9Z98+s+/+Ddf/Murcs6/6Zdf/M" +
               "uvcs6/6Zdf/Muvcs6/6Zde9Z98+s+866/+CsuW/mufZmvplv5rn2P8f+3/9n2N/m+/9A07vvL/O++GbeF/O+mP8D8g8o90Z1YgIAAAAASUVORK5C" +
               "YII="
    }
}
